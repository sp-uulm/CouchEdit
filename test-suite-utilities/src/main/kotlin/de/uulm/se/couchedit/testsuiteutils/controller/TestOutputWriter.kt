package de.uulm.se.couchedit.testsuiteutils.controller

import com.google.common.collect.HashBasedTable
import com.google.common.collect.HashMultimap
import de.uulm.se.couchedit.testsuiteutils.TestUtilityConstants
import de.uulm.se.couchedit.testsuiteutils.model.TestInstanceInfo
import de.uulm.se.couchedit.testsuiteutils.model.TestStepInfo
import de.uulm.se.couchedit.testsuiteutils.model.WrappedTestInstanceInfo
import org.junit.jupiter.api.Order
import java.io.BufferedWriter
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

internal object TestOutputWriter {
    private const val COL_SEPARATOR = ","
    private const val LINE_SEPARATOR = "\n"

    /* ---- Strings for Test Instance Information ---- */
    private const val TEST_CASE_ID_COL = "Test_ID"
    private const val TEST_CASE_CLASS_COL = "Test_CLASS"
    private const val TEST_CASE_DISPLAY_COL = "Test_DISPLAY_NAME"

    /* ---- Strings for Test Step Information ---- */
    private const val TEST_STEP_NUMBER_COL = "Step_NO"
    private const val TEST_STEP_METHOD_NAME_COL = "Step_METHOD_NAME"
    private const val TEST_STEP_ACTION_TYPE = "Step_ACTION"
    private const val TEST_STEP_DIFF_COLLECTION_SIZE = "Step_DIFF_COLLECTION_SIZE"
    private const val TEST_STEP_DURATION = "${TestUtilityConstants.RESULT_PREFIX}TOTAL_DURATION"
    private val DATE_FORMAT = SimpleDateFormat("yyyyMMddHHmmss")

    private val DEFAULT_OUTPUT_PATH by lazy {
        val sysTmpDir = System.getProperty("java.io.tmpdir")

        return@lazy "$sysTmpDir/couch-edit-tests"
    }

    private val outputPath = System.getProperty("outDir", DEFAULT_OUTPUT_PATH)

    /**
     * Writes test results to CSV files, each file representing the results of one test case (Method)
     * in the test suite.
     */
    fun write(testSuiteName: String, infos: HashMultimap<WrappedTestInstanceInfo, TestStepInfo>) {
        val dirPath = Paths.get(outputPath, DATE_FORMAT.format(Date()) + "_$testSuiteName")

        val methodTable = HashBasedTable.create<KFunction<*>, WrappedTestInstanceInfo, MutableList<TestStepInfo>>()

        for ((instance, step) in infos.entries()) {
            val list = methodTable.get(step.method, instance) ?: run {
                val list = mutableListOf<TestStepInfo>()
                methodTable.put(step.method, instance, list)
                return@run list
            }

            list.add(step)
        }

        Files.createDirectories(dirPath)

        println("TESTSUITE: Start writing test results to $dirPath")

        for ((method, results) in methodTable.rowMap()) {
            val order = method.findAnnotation<Order>()?.value ?: 0

            val fileName = "$order.csv"

            val columns = mutableSetOf<String>()

            val rows = mutableListOf<Map<String, String>>()

            for ((instanceInfo, stepInfos) in results) {
                val instanceColumns = instanceInfoToStringMap(instanceInfo)

                for (stepInfo in stepInfos) {
                    val row = instanceColumns.plus(
                            stepInfoToStringMap(stepInfo)
                    )

                    columns.addAll(row.keys)
                    rows.add(row)
                }
            }

            val csvString = convertToCSV(columns, rows)

            val filePath = Paths.get(dirPath.toAbsolutePath().toString(), fileName)

            writeToFile(filePath, csvString)

            println("TESTSUITE: Created output file $filePath")
        }

        println("TESTSUITE: Finished writing to $dirPath")
    }

    /**
     * Converts a [TestInstanceInfo] instance to a map of column names and column values
     *
     * @param testInstanceInfo The test instance to be converted to a String Map
     * @return Map of (Column Name, Column Value)
     */
    private fun instanceInfoToStringMap(testInstanceInfo: WrappedTestInstanceInfo): Map<String, String> {
        val ret = mutableMapOf<String, String>()

        ret[TEST_CASE_ID_COL] = testInstanceInfo.testInstanceInfo.id
        ret[TEST_CASE_CLASS_COL] = testInstanceInfo.clazz.simpleName
        ret[TEST_CASE_DISPLAY_COL] = testInstanceInfo.testInstanceInfo.displayName

        val details = testInstanceInfo.testInstanceInfo.testDetails

        val detailType = details::class

        for (property in detailType.memberProperties) {
            @Suppress("UNCHECKED_CAST") val castedProperty = property as KProperty1<Any, *>

            ret[property.name] = castedProperty.get(details).toString()
        }

        return ret
    }

    /**
     * Converts a [TestStepInfo] instance to a map of column names and column values
     */
    private fun stepInfoToStringMap(x: TestStepInfo): Map<String, String> {
        val ret = mutableMapOf<String, String>()

        ret[TEST_STEP_NUMBER_COL] = x.stepNumber.toString()
        ret[TEST_STEP_METHOD_NAME_COL] = x.method.name
        ret[TEST_STEP_ACTION_TYPE] = x.action.name
        ret[TEST_STEP_DIFF_COLLECTION_SIZE] = x.inputDiffCollectionSize.toString()
        ret[TEST_STEP_DURATION] = x.durationMs.toString()

        for ((k, v) in x.additionalProperties) {
            ret[k] = v.toString()
        }

        return ret
    }

    private fun convertToCSV(cols: Collection<String>, rows: List<Map<String, String>>): String {
        val headerText = cols.joinToString(COL_SEPARATOR)

        val rowTexts = rows.map { row ->
            val colValues = cols.map { col ->
                "\"" + (row[col] ?: "") + "\""
            }

            return@map colValues.joinToString(COL_SEPARATOR)
        }

        return headerText + LINE_SEPARATOR + rowTexts.joinToString(LINE_SEPARATOR)
    }

    private fun writeToFile(path: Path, content: String) {
        val bufferedWriter = BufferedWriter(FileWriter(path.toString()))

        bufferedWriter.write(content)

        bufferedWriter.close()
    }
}
