from typing import List, Dict

from resultinterpretation.model import TestStepResult


class TestSuiteResult:
    """
    Aggregated results of the execution of one CouchEditTestSuite at the end of a test run
    """
    def __init__(self, suiteName: str, steps: List[TestStepResult], suiteInfo: Dict):
        self.suiteInfo = suiteInfo
        self.suiteName = suiteName
        self.steps = steps
