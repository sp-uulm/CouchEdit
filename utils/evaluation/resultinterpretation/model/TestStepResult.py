from typing import List

from resultinterpretation.model import TestStepResultItem


class TestStepResult:
    """
    Aggregate results of one test step over the course of multiple executions of one CouchEditTestSuite
    """
    def __init__(self, stepNumber: int, resultItems: List[TestStepResultItem]):
        self.stepNumber = stepNumber
        self.resultItems = resultItems
