package org.lakehouse.taskexecutor.executionmodule.body;

import com.hubspot.jinjava.Jinjava;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.lakehouse.client.api.constant.Configuration;
import org.lakehouse.client.api.constant.DataSetPropertyKeys;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.jinja.java.JinJavaFactory;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.DataSourceManipulator;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.UnsuportedDataSourceException;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.WriteException;
import org.lakehouse.taskexecutor.executionmodule.body.transformer.DataTransformer;
import org.lakehouse.taskexecutor.executionmodule.body.transformer.TransformationException;
import org.lakehouse.taskexecutor.executionmodule.body.transformer.TransformerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TransformationSparkProcessorBody extends SparkProcessorBodyAbstract{
    public TransformationSparkProcessorBody(BodyParam bodyParam) {
        super(bodyParam);
    }
    private final Jinjava jinjava = new JinJavaFactory().getJinjava();

    private void rebuildKeyBind(Map<String, DataSourceManipulator> sources, DataSourceManipulator targetDataSourceManipulator){
            Set<DataSourceManipulator> dataSourceManipulators = new HashSet<>(sources.values());
            dataSourceManipulators.add(targetDataSourceManipulator);

        dataSourceManipulators.forEach(dsm ->
        getBodyParam().getKeyBind().put(
                SystemVarKeys
                        .buildSourceTableFullName(
                                dsm.getDataSetDTO().getNameSpaceKeyName(),
                                dsm.getDataSetDTO().getKeyName()),
                dsm.getCatalogTableFullName()));

    }
    @Override
    public void run() throws TaskFailedException {

        try {
            //input
            Map<String,DataSourceManipulator> sourceDataSourceManipulators = getBodyParam().getSourceDataSourceManipulatorMap();

            DataSourceManipulator resultDataSourceManipulator = getBodyParam().getTargetDataSourceManipulator();

            // reset keybinds
            rebuildKeyBind(sourceDataSourceManipulators,resultDataSourceManipulator);
            //transformation
            TransformerFactory f = new TransformerFactory();
            DataTransformer t = f.buildDataTransformer(
                    jinjava.render(
                        getBodyParam().getScripts().get(0)
                            ,getBodyParam().getKeyBind()));
            Dataset<Row> transformResult = t.transform(sourceDataSourceManipulators,resultDataSourceManipulator);
            transformResult.show();
            //save
            resultDataSourceManipulator.write(
                    transformResult,
                    new HashMap<>(),
                    getModificationRule(resultDataSourceManipulator.getDataSetDTO())
                    );
        }catch ( TransformationException | WriteException ue){
            throw  new TaskFailedException(ue);
        }
    }


    private Configuration.ModificationRule getModificationRule(DataSetDTO dataSetDTO){
        return Configuration
                .ModificationRule
                .valueOf(dataSetDTO
                        .getProperties()
                        .getOrDefault(
                                DataSetPropertyKeys.Key.TRANSFORM_LOCATION_MODE.label,
                                Configuration.ModificationRule.append.getValue()));
    }

}
