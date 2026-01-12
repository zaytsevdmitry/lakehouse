package org.lakehouse.taskexecutor.executionmodule.body.transformer;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.lakehouse.taskexecutor.api.datasource.exception.ExecuteException;
import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.SparkSQLDataSourceManipulator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SQLDataTransformer extends DataTransformerAbstract {
    private final String script;
    private final String scriptCommandDelimiter;

    public static String defaultDelimiter = "\n;";
    public SQLDataTransformer(
            String script,
            String scriptCommandDelimiter) {
        this.script = script;
        this.scriptCommandDelimiter = scriptCommandDelimiter;
    }
    private List<String> splitScripts(){
        String delimiter = scriptCommandDelimiter;
        if (scriptCommandDelimiter == null || scriptCommandDelimiter.isBlank()) delimiter = defaultDelimiter;
        return List.of(script.split(delimiter));
    }

    @Override
    public Dataset<Row> transform(
            Map<String, SparkSQLDataSourceManipulator> sourceDataSourceManipulators,
            SparkSQLDataSourceManipulator targetSparkDataSourceManipulator) throws TransformationException {

        List<String> commands =  splitScripts();
        String mainSQL = commands.get(commands.size()-1);
        List<String> preSQLs = new ArrayList<>();
        if (commands.size() > 1){
            for(int i=0; i < commands.size()-1; i++) {
                preSQLs.add(commands.get(i));
            }
        }
        try {
            for (String preSQL : preSQLs) {
                targetSparkDataSourceManipulator.executeUtils().execute(preSQL);
            }

            Dataset<Row> result = targetSparkDataSourceManipulator.executeUtils().executeQuery(mainSQL);
            result.show();
            return result;
        }catch (ExecuteException e){
            throw new TransformationException(e);
        }
    }
}
