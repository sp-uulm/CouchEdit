import csv
from collections import OrderedDict
from pathlib import Path
from typing import List, Tuple, Dict

from resultinterpretation.model import TestStepResult, TestStepResultItem


class CSVFileExtractor:
    def __init__(self, independentName: str, dependentPrefix: str):
        """
        :param independentName: Name of the independent variable column
        :param dependentPrefix: Prefix that all dependent variable columns should have
        """
        self.__independentName = independentName
        self.__dependentPrefix = dependentPrefix

    def extract(self, paths: List[Path]) -> List[TestStepResultItem]:
        columnDict = {}

        for path in paths:
            with open(str(path.absolute())) as csvFile:
                reader = csv.reader(csvFile, delimiter=",")

                lineNumber = 0

                independentColumnNumber = 0
                dependentColumnNumbers = {}

                for line in reader:
                    if lineNumber == 0:
                        independentColumnNumber = line.index(self.__independentName)
                        for index, column in enumerate(line, 0):
                            if column.startswith(self.__dependentPrefix):
                                dependentColumnNumbers[index] = column
                    else:
                        try:
                            independentColumnValue = line[independentColumnNumber]

                            for columnIndex, dependentVariableName in dependentColumnNumbers.items():
                                if dependentVariableName not in columnDict:
                                    columnDict[dependentVariableName] = {}

                                valueDictForColumn = columnDict[dependentVariableName]

                                if independentColumnValue not in valueDictForColumn:
                                    valueDictForColumn[independentColumnValue] = []

                                dependentVariableValueList = valueDictForColumn[independentColumnValue]

                                try:
                                    dependentVariableValue = line[columnIndex]

                                    dependentVariableValueList.append(dependentVariableValue)
                                except IndexError:
                                    raise Exception("Row {0} did not contain index of dependent variable {1}."
                                                    .format(str(line), str(dependentVariableValue)))
                        except IndexError:
                            raise Exception("Row {0} did not contain index of independent column {1}."
                                            .format(str(line), str(self.__independentName)))

                    lineNumber += 1

        ret = []

        for dataSetName, values in columnDict.items():
            aggregates = {int(k): self.calculateAggregate(v) for k, v in values.items()}

            resultObject = TestStepResultItem(
                dataSetName,
                OrderedDict(sorted(aggregates.items()))
            )

            ret.append(resultObject)

        return ret

    def calculateAggregate(self, data: List):
        return sum([float(item) for item in data]) / len(data)

