package org.lakehouse.taskexecutor.executionmodule.body.transformer;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.DataSourceManipulator;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.ReadException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SQLDataTransformer extends DataTransformerAbstract {
    private final String script;
    private final String scriptCommandDelimiter;

    public static String defaultDelimiter = "\n;";
    public SQLDataTransformer(
         //   SparkSession sparkSession,
            String script,
            String scriptCommandDelimiter) {
        //super(sparkSession);
        this.script = script;
        this.scriptCommandDelimiter = scriptCommandDelimiter;
    }
    private List<String> splitScripts(){
        String delimiter = scriptCommandDelimiter;
        if (scriptCommandDelimiter == null || scriptCommandDelimiter.isBlank()) delimiter = defaultDelimiter;
        return List.of(script.split(delimiter));
    }
  /*  private Map<String, Dataset<Row>> loadDataSets(List<DataSourceManipulator> dataSourceManipulators) throws ReadException {
        Map<String, Dataset<Row>> result = new HashMap<>();
        for (DataSourceManipulator dataSourceManipulator:dataSourceManipulators) {
            String catalogName = dataSourceManipulator.getDataSourceDTO().getKeyName();
            String catalogDesc = dataSourceManipulator.getDataSourceDTO().getDescription();
            SparkSession sparkSession = dataSourceManipulator.getSparkSession();
            sparkSession
                    .catalog()
                    .listCatalogs();

            sparkSession
                    .catalog()
                    .listCatalogs().show();
            Dataset<Row> dataset = dataSourceManipulator.read(new HashMap<>());
            result.put(
                    dataSourceManipulator
                            .getDataSetDTO()
                            .getKeyName(),
                    dataset);
        }

        return result;
    }
*/
    private void loadViews(Map<String, Dataset<Row>> datasetMap){
        for( String key:datasetMap.keySet()){
            datasetMap.get(key).createOrReplaceTempView(key);
        }
    }
    @Override
    public Dataset<Row> transform(
            Map<String,DataSourceManipulator> sourceDataSourceManipulators,
            DataSourceManipulator targetDataSourceManipulator) throws TransformationException {


        List<String> commands =  splitScripts();
        String mainSQL = commands.get(commands.size()-1);
        List<String> preSQLs = new ArrayList<>();
        if (commands.size() > 1){
            for(int i=0; i < commands.size()-1; i++) {
                preSQLs.add(commands.get(i));
            }
        }

        for (String preSQL:preSQLs){
            targetDataSourceManipulator.executeQuery(preSQL, false);
        }

        Dataset<Row> result = targetDataSourceManipulator.executeQuery(mainSQL, false);
        sourceDataSourceManipulators.values().forEach(dataSourceManipulator -> {
            try {
                dataSourceManipulator.read(new HashMap<>()).show();
            } catch (ReadException e) {
                throw new RuntimeException(e);
            }
        });
        result.show();
        return result;
    }
}
