package org.lakehouse.client.api.factory;

import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.factory.dialect.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;

public class TableDialectFactory {
    private  Map<Types.EngineType, Map<Types.Engine,Class<? extends TableDialect>>> routeMap = Map.of(
            Types.EngineType.database, Map.of(Types.Engine.postgres, PostgresJdbcTableDialect.class),
            Types.EngineType.spark, Map.of(Types.Engine.iceberg, SparkIcebergTableDialect.class)
    );
    private  Class<? extends TableDialect> findClass(Types.EngineType engineType, Types.Engine engine){
        if (routeMap.containsKey(engineType)){
            return routeMap.get(engineType).getOrDefault(engine, AbstractTableDialect.class);
        }else{
            return AbstractTableDialect.class;
        }
    }
    private TableDialect createInstance(
            Class<? extends TableDialect> tableDialectClass,
            DataSetDTO targetSetDTO,
            Map<String,DataSetDTO> foreignDataSetDTOs)
            throws InvocationTargetException,
            InstantiationException,
            IllegalAccessException,
            NoSuchMethodException {
        Constructor<? extends TableDialect> constructor = tableDialectClass.getConstructor(TableDialectParameter.class);
        TableDialectParameter tableDialectParameter = new TableDialectParameter(targetSetDTO,foreignDataSetDTOs);
        return constructor.newInstance(tableDialectParameter);
    }
    public  TableDialect buildTableDialect(
            DataSourceDTO targetSourceDTO,
            DataSetDTO targetSetDTO,
            Map<String,DataSetDTO> foreignDataSetDTOs) throws TaskFailedException {


                 try {
                     return createInstance(
                             findClass(
                                     targetSourceDTO.getEngineType(),
                                     targetSourceDTO.getEngine()),
                             targetSetDTO,
                             foreignDataSetDTOs);
                 } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                          NoSuchMethodException e) {
                     throw new TaskFailedException(e);
                 }


             }
}
