## **Precondition**

* [Install via pip](install-from-pip.md)

## **Use an Interactive Shell**
 * ```export SPARK_HOME=path to spark-1.6.3-bin-hadoop2.6```
 * Type `python` in the command line to start a REPL
 * __NOTE__: Only __Python 2.7__ and __Python 3.5__ are supported for now.

## **Use Jupyter Notebook**
 * ```export SPARK_HOME=path to spark-1.6.3-bin-hadoop2.6```
 * Start jupyter notebook as you normally do, e.g.
```bash
 jupyter notebook --notebook-dir=./ --ip=* --no-browser
```


<a name="code.verification"></a>
## Example code to verify if BigDL can run successfully ##
```python
from bigdl.util.common import *
from pyspark import SparkContext
from bigdl.nn.layer import *
import bigdl.version
 
# create sparkcontext with bigdl configuration
sc = SparkContext.getOrCreate(conf=create_spark_conf()) 
init_engine() # prepare the bigdl environment 
bigdl.version.__version__ # Get the current BigDL version
linear = Linear(2, 3) # Try to create a Linear layer
 
```

## BigDL Configuration
- Increase memory
  - export SPARK_DRIVER_MEMORY=20g



