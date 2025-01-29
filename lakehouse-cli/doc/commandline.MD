# Command types


```
show one|all <object type> <name|id>
/

upload <objectType> <filePath>
/

download <objectType> <name|id> <filePath>  
/

delete <objectType> <name|id> 
/

exit
/

help
/

```


show - print requested data

	one - print json of requested object

	all - print list of objects by requested type
	

<objectType> - posible value is one of: 
		project 
		datastore 
		dataset 
		schedule  
		taskexecutionservicegroup
		scheduledtask
		

<name|id> - key identifier for requested object


<filePath> - full path for file 

