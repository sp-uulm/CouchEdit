import csv
from pathlib import Path
from typing import List

from resultinterpretation.model import TestSuiteResult
from resultwriter import ResultWriter


class CSVResultWriter(ResultWriter):
    def _doWriteResult(self, suiteDir: Path, result: TestSuiteResult):
        for step in result.steps:
            stepFilePath = suiteDir / 'Step_{0}.csv'.format(step.stepNumber)

            columnNames = set()

            rows = {}

            for item in step.resultItems:
                columnNames.add(item.resultValueName)

            orderedColumnNames = list(sorted(columnNames))

            for item in step.resultItems:
                for independentValue, dependentValue in item.values.items():
                    if independentValue not in rows:
                        rows[independentValue] = self.__initializeRow(len(step.resultItems))

                    row = rows[independentValue]
                    row[orderedColumnNames.index(item.resultValueName)] = dependentValue

            with open(str(stepFilePath), 'w') as csvFile:
                writer = csv.writer(csvFile)

                writer.writerow([result.suiteInfo['independentVariable']] + list(orderedColumnNames))
                writer.writerows([[k] + v for k, v in rows.items()])

    def __initializeRow(self, size: int) -> List:
        ret = []

        for i in range(0, size):
            ret.append('')

        return ret