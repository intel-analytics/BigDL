"""SparseAdam optimizer implementation."""

import tensorflow.compat.v2 as tf
from keras import backend_config
from keras.optimizer_v2 import optimizer_v2
from tensorflow.python.util.tf_export import keras_export
import tensorflow


class SparseAdam(tensorflow.keras.optimizers.Adam):
    r"""Optimizer that implements the SparseAdam algorithm.

  Args:
    learning_rate: A `Tensor`, floating point value, or a schedule that is a
      `tf.keras.optimizers.schedules.LearningRateSchedule`, or a callable
      that takes no arguments and returns the actual value to use, The
      learning rate. Defaults to 0.001.
    beta_1: A float value or a constant float tensor, or a callable
      that takes no arguments and returns the actual value to use. The
      exponential decay rate for the 1st moment estimates. Defaults to 0.9.
    beta_2: A float value or a constant float tensor, or a callable
      that takes no arguments and returns the actual value to use, The
      exponential decay rate for the 2nd moment estimates. Defaults to 0.999.
    epsilon: A small constant for numerical stability. This epsilon is
      "epsilon hat" in the Kingma and Ba paper (in the formula just before
      Section 2.1), not the epsilon in Algorithm 1 of the paper. Defaults to
      1e-7.
    amsgrad: Boolean. Whether to apply AMSGrad variant of this algorithm from
      the paper "On the Convergence of Adam and beyond". Defaults to `False`.
    name: Optional name for the operations created when applying gradients.
      Defaults to `"Adam"`.
    **kwargs: Keyword arguments. Allowed to be one of
      `"clipnorm"` or `"clipvalue"`.
      `"clipnorm"` (float) clips gradients by norm; `"clipvalue"` (float) clips
      gradients by value.
  """

    _HAS_AGGREGATE_GRAD = True

    def __init__(self,
                 learning_rate=0.001,
                 beta_1=0.9,
                 beta_2=0.999,
                 epsilon=1e-7,
                 amsgrad=False,
                 name='SparseAdam',
                 **kwargs):

        super().__init__(
            learning_rate=learning_rate,
            beta_1=beta_1,
            beta_2=beta_2,
            epsilon=epsilon,
            amsgrad=amsgrad,
            name=name,
            **kwargs,
        )

    def _create_slots(self, var_list):
        # Create slots for the first and second moments.
        # Separate for-loops to respect the ordering of slot variables from v1.
        for var in var_list:
            self.add_slot(var, 'm')
        for var in var_list:
            self.add_slot(var, 'v')
        if self.amsgrad:
            for var in var_list:
                self.add_slot(var, 'vhat')

    def _prepare_local(self, var_device, var_dtype, apply_state):
        super(SparseAdam, self)._prepare_local(var_device, var_dtype, apply_state)

        local_step = tf.cast(self.iterations + 1, var_dtype)
        beta_1_t = tf.identity(self._get_hyper('beta_1', var_dtype))
        beta_2_t = tf.identity(self._get_hyper('beta_2', var_dtype))
        beta_1_power = tf.pow(beta_1_t, local_step)
        beta_2_power = tf.pow(beta_2_t, local_step)
        lr = (apply_state[(var_device, var_dtype)]['lr_t'] *
              (tf.sqrt(1 - beta_2_power) / (1 - beta_1_power)))
        apply_state[(var_device, var_dtype)].update(
            dict(
                lr=lr,
                epsilon=tf.convert_to_tensor(
                    self.epsilon, var_dtype),
                beta_1_t=beta_1_t,
                beta_1_power=beta_1_power,
                one_minus_beta_1_t=1 - beta_1_t,
                beta_2_t=beta_2_t,
                beta_2_power=beta_2_power,
                one_minus_beta_2_t=1 - beta_2_t))

    def set_weights(self, weights):
        params = self.weights
        # If the weights are generated by Keras V1 optimizer, it includes vhats
        # even without amsgrad, i.e, V1 optimizer has 3x + 1 variables, while V2
        # optimizer has 2x + 1 variables. Filter vhats out for compatibility.
        num_vars = int((len(params) - 1) / 2)
        if len(weights) == 3 * num_vars + 1:
            weights = weights[:len(params)]
        super(SparseAdam, self).set_weights(weights)

    def _resource_apply_dense(self, grad, var, apply_state=None):
        var_device, var_dtype = var.device, var.dtype.base_dtype
        coefficients = ((apply_state or {}).get((var_device, var_dtype))
                        or self._fallback_apply_state(var_device, var_dtype))

        m = self.get_slot(var, 'm')
        v = self.get_slot(var, 'v')

        if not self.amsgrad:
            return tf.raw_ops.ResourceApplyAdam(
                var=var.handle,
                m=m.handle,
                v=v.handle,
                beta1_power=coefficients['beta_1_power'],
                beta2_power=coefficients['beta_2_power'],
                lr=coefficients['lr_t'],
                beta1=coefficients['beta_1_t'],
                beta2=coefficients['beta_2_t'],
                epsilon=coefficients['epsilon'],
                grad=grad,
                use_locking=self._use_locking)
        else:
            vhat = self.get_slot(var, 'vhat')
            return tf.raw_ops.ResourceApplyAdamWithAmsgrad(
                var=var.handle,
                m=m.handle,
                v=v.handle,
                vhat=vhat.handle,
                beta1_power=coefficients['beta_1_power'],
                beta2_power=coefficients['beta_2_power'],
                lr=coefficients['lr_t'],
                beta1=coefficients['beta_1_t'],
                beta2=coefficients['beta_2_t'],
                epsilon=coefficients['epsilon'],
                grad=grad,
                use_locking=self._use_locking)

    def _resource_apply_sparse(self, grad, var, indices, apply_state=None):
        var_device, var_dtype = var.device, var.dtype.base_dtype
        coefficients = ((apply_state or {}).get((var_device, var_dtype))
                        or self._fallback_apply_state(var_device, var_dtype))

        import tensorflow as tf

        with tf.name_scope("update_sparse_mt"):
            # m_t = beta1 * m + (1 - beta1) * g_t
            m = self.get_slot(var, 'm')
            m_scaled_g_values = grad * coefficients['one_minus_beta_1_t']
            m_sparse = tf.gather(m, indices=indices)

            m_sparse_scaled = m_sparse * (coefficients['beta_1_t'] - 1)
            # m_t = state_ops.assign(m, m * coefficients['beta_1_t'],
            #                        use_locking=self._use_locking)
            # with ops.control_dependencies([m_t]):

            m_t = self._resource_scatter_add(m, indices, m_scaled_g_values + m_sparse_scaled)
            m_t_sparse = m_sparse * coefficients['beta_1_t'] + m_sparse_scaled

        with tf.name_scope("update_sparse_vt"):
            # v_t = beta2 * v + (1 - beta2) * (g_t * g_t)
            v = self.get_slot(var, 'v')
            v_scaled_g_values = (grad * grad) * coefficients['one_minus_beta_2_t']
            v_sparse = tf.gather(v, indices=indices)
            v_sparse_scaled = v_sparse * (coefficients['beta_2_t'] - 1)

            # v_t = state_ops.assign(v, v * coefficients['beta_2_t'],
            #                        use_locking=self._use_locking)
            # with ops.control_dependencies([v_t]):
            v_t = self._resource_scatter_add(v, indices, v_scaled_g_values + v_sparse_scaled)

            v_t_sparse = v_sparse * coefficients['beta_2_t'] + v_scaled_g_values

        if not self.amsgrad:
            with tf.name_scope("update_sparse_var"):
                v_sqrt_sparse = tf.sqrt(v_t_sparse)
                var_update = self._resource_scatter_add(
                    var, indices, -1 * coefficients['lr'] * m_t_sparse / (v_sqrt_sparse + coefficients['epsilon']))
                return tf.group(*[var_update, m_t, v_t])
        else:
            raise ValueError("do not support amsgrad for now")
            v_hat = self.get_slot(var, 'vhat')
            v_hat_t = math_ops.maximum(v_hat, v_t)
            with ops.control_dependencies([v_hat_t]):
                v_hat_t = state_ops.assign(
                    v_hat, v_hat_t, use_locking=self._use_locking)
            v_hat_sqrt = math_ops.sqrt(v_hat_t)
            var_update = state_ops.assign_sub(
                var,
                coefficients['lr'] * m_t / (v_hat_sqrt + coefficients['epsilon']),
                use_locking=self._use_locking)
            return control_flow_ops.group(*[var_update, m_t, v_t, v_hat_t])

    def get_config(self):
        config = super(SparseAdam, self).get_config()
        config.update({
            'learning_rate': self._serialize_hyperparameter('learning_rate'),
            'decay': self._initial_decay,
            'beta_1': self._serialize_hyperparameter('beta_1'),
            'beta_2': self._serialize_hyperparameter('beta_2'),
            'epsilon': self.epsilon,
            'amsgrad': self.amsgrad,
        })
        return config