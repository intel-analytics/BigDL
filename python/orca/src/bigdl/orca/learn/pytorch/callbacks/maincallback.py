#
# Copyright 2016 The BigDL Authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
from .base import Callback

class MainCallback(Callback):
    """
    MainCallback is a one-of-a-kind callback that contains hook functions:
        - `on_iter_forward`
        - `on_iter_backward`
        - `on_lr_adjust`

    These methods are somewhat special, because only one special MainCallback
    should be allowed to implement these methods among all callbacks, otherwise
    there will propagate forward and backward twice.
    """
    def on_iter_forward(self, runner):
        *features, target = self.batch
        self.output = self.model(*features)
        # Ensure `target` and `output` are always in a list format.
        targetL = [target] if not isinstance(target, (list, tuple)) else target
        outputL = [self.output] if not isinstance(self.output, (list, tuple)) else self.output
        self.loss = self.criterion(*outputL, *targetL)

    def on_iter_backward(self, runner):
        runner.optimizer.zero_grad()
        runner.loss.backward()
        runner.optimizer.step()
    
    def on_lr_adjust(self, runner):
        if runner.lr_scheduler is not None:
           runner.lr_scheduler.step()

    def on_train_forward(self, runner):
        self.on_iter_forward(runner)

    def on_val_forward(self, runner):
        self.on_iter_forward(runner)

    def on_train_backward(self, runner):
        self.on_iter_backward(runner)

    def on_val_backward(self, runner):
        self.on_iter_backward(runner)
