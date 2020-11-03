#! /bin/python3

import argparse
from datetime import datetime

from pathlib import Path

from resultinterpretation import ResultInterpreter
from resultwriter.tex import TexResultWriter
from testrunner import TestSuiteRunner

parser = argparse.ArgumentParser("CouchEdit evaluation utility - Run + interpret")

parser.add_argument('-d', '--dir', dest='directory')
parser.add_argument('--current-datetime', dest='appendCurrentDateTime', action='store_true')
parser.add_argument('-t', '--task', dest='task')
parser.add_argument('-e', '--tests', dest='tests', default='*')
parser.add_argument('-n', '--num', dest='num', default='1', type=int)

args = parser.parse_args()


runner = TestSuiteRunner()

testRuns = args.num

workPath = Path(args.directory).absolute()

if args.appendCurrentDateTime:
    currentDateTime = datetime.now()

    workPath = workPath / "run_{0}".format(currentDateTime.strftime("%Y%m%d%H%M%S"))

testResultPath = workPath / 'raw'
outputPath = workPath / 'out'

runCount = 1
while testRuns > 0:
    print("RUN {0} of {1}".format(runCount, args.num))

    runner.runTest(testResultPath, args.task, args.tests)

    testRuns -= 1
    runCount += 1

interpreter = ResultInterpreter(testResultPath)

results = interpreter.interpretResults()

writer = TexResultWriter()

for result in results:
    writer.writeResult(outputPath, result)
