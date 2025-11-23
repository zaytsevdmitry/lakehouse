package org.lakehouse.taskexecutor.executionmodule.body;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.lakehouse.client.api.constant.Configuration;
import org.lakehouse.client.api.constant.DataSetPropertyKeys;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.DataStoreManipulatorFactory;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.UnsuportedDataSourceException;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.ReadException;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.WriteException;
import org.lakehouse.taskexecutor.executionmodule.body.entity.DataSetItem;
import org.lakehouse.taskexecutor.executionmodule.body.transformer.DataTransformer;
import org.lakehouse.taskexecutor.executionmodule.body.transformer.TransformationException;
import org.lakehouse.taskexecutor.executionmodule.body.transformer.TransformerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TransformationSparkProcessorBody extends SparkProcessorBodyAbstract{
    public TransformationSparkProcessorBody(BodyParam bodyParam) {
        super(bodyParam);
    }


    @Override
    public void run() throws TaskFailedException {

        try {
            //input
            List<DataSetItem> dataSetItems = getDataSetItems();
            DataSetItem resultDataSetItem = getResultDataSetItem(dataSetItems);

            //transformation
            TransformerFactory f = new TransformerFactory();
            DataTransformer t = f.buildDataTransformer(getTaskProcessorConfigDTO());
            Dataset<Row> transformResult = t.transform(dataSetItems,resultDataSetItem);

            //save
            resultDataSetItem.getDataSourceManipulator().write(
                    transformResult,
                    getTransformLocation(resultDataSetItem),
                    getTransformOptions(resultDataSetItem),
                    getModificationRule(resultDataSetItem)
                    );
        }catch (UnsuportedDataSourceException | ReadException | TransformationException | WriteException ue){
            throw  new TaskFailedException(ue);
        }
    }

    private List<DataSetItem> getDataSetItems() throws ReadException, UnsuportedDataSourceException {
        return new DataSetItemFactory(
                new DataStoreManipulatorFactory(),
                getBodyParam()
                        .getSparkSession())
                .buildDataSetItems(getTaskProcessorConfigDTO());

    }

    private DataSetItem getResultDataSetItem(List<DataSetItem> dataSetItems){
        return dataSetItems
                .stream()
                .filter(dataSetItem -> dataSetItem.getDataSetDTO().equals(getTaskProcessorConfigDTO().getTargetDataSet()))
                .findFirst().get();

    }
    private Configuration.ModificationRule getModificationRule(DataSetItem dataSetItem){
        return Configuration
                .ModificationRule
                .valueOf(dataSetItem
                        .getDataSetDTO()
                        .getProperties()
                        .get(DataSetPropertyKeys.Key.TRANSFORM_LOCATION_MODE));
    }
    private String getTransformLocation(DataSetItem dataSetItem){
        return dataSetItem.getDataSetDTO().getProperties().get(DataSetPropertyKeys.Key.TRANSFORM_LOCATION);
    }
    private Map<String,String> getTransformOptions(DataSetItem dataSetItem){
        return dataSetItem
                .getDataSetDTO()
                .getProperties()
                .entrySet()
                .stream()
                .filter(stringStringEntry -> stringStringEntry.getKey().startsWith(DataSetPropertyKeys.Prefix.transform.toString()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
