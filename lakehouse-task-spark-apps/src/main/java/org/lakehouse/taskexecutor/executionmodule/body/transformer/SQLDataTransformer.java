package org.lakehouse.taskexecutor.executionmodule.body.transformer;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.ReadException;
import org.lakehouse.taskexecutor.executionmodule.body.entity.DataSetItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SQLDataTransformer extends DataTransformerAbstract {
    private final String script;
    private final String scriptCommandDelimiter;

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
        if (scriptCommandDelimiter == null || scriptCommandDelimiter.isBlank()) delimiter = "\n;";
        return List.of(script.split(delimiter));
    }
    private Map<String, Dataset<Row>> loadDataSets(List<DataSetItem> dataSetItems) throws ReadException {
        Map<String, Dataset<Row>> result = new HashMap<>();
        for (DataSetItem dataSetItem:dataSetItems) {
            Dataset<Row> dataset = dataSetItem.getDataSourceManipulator().read("", new HashMap<>());
            result.put(dataSetItem.getDataSetDTO().getKeyName(), dataset);
        }

        return result;
    }

    private void loadViews(Map<String, Dataset<Row>> datasetMap){
        for( String key:datasetMap.keySet()){
            datasetMap.get(key).createOrReplaceTempView(key);
        }
    }
    @Override
    public Dataset<Row> transform(List<DataSetItem> sourceDataSetItems, DataSetItem targetDataSetItem) throws TransformationException {
        try {
            loadViews(loadDataSets(sourceDataSetItems));
        } catch (ReadException e) {
            throw new TransformationException(e);
        }
        List<String> commands =  splitScripts();
        String mainSQL = commands.get(commands.size()-1);
        List<String> preSQLs = new ArrayList<>();
        if (commands.size() > 1){
            for(int i=0; i < commands.size()-1; i++) {
                preSQLs.add(commands.get(i));
            }
        }

        for (String preSQL:preSQLs){
            targetDataSetItem.getDataSourceManipulator().executeQuery(preSQL, false);
        }

        Dataset<Row> result = targetDataSetItem.getDataSourceManipulator().executeQuery(mainSQL, false);
        return result;
    }
}
