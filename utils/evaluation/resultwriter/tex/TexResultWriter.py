from pathlib import Path

from resultinterpretation.model import TestSuiteResult
from resultwriter import ResultWriter


class TexResultWriter(ResultWriter):
    def _doWriteResult(self, suiteDir: Path, result: TestSuiteResult):
        for step in result.steps:
            stepDir = suiteDir / 'Step_{0}'.format(step.stepNumber)

            stepDir.mkdir(parents=True)

            for dataRow in step.resultItems:
                resultFile = stepDir / "{0}_{1}".format(step.stepNumber, dataRow.resultValueName)

                with resultFile.open(mode='w+') as f:
                    for key, value in dataRow.values.items():
                        f.write("({0},{1})".format(str(key), str(value)))
