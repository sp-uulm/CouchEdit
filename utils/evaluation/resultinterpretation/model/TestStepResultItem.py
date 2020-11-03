from typing import Dict


class TestStepResultItem:
    """
    Aggregate values for one column of the execution of one test step, associated with the independent variable
    """
    def __init__(self, resultValueName: str, values: Dict[str, str]):
        """
        :param resultValueName: The name of the column in the test result output
        :param values: The aggregate value of the resultValueName column, with the key being the independent variable.
        """
        self.resultValueName = resultValueName
        self.values = values
