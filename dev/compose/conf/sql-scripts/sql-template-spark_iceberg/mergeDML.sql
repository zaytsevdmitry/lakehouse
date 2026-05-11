{%set targetDataSet=dataSets[targetDataSetKeyName]%}MERGE INTO  {{ refCat(targetDataSetKeyName) }} t
	USING ({{ script }}) q
	ON ({{ extractMergeOn(targetDataSet,'t','q') }})
	WHEN MATCHED THEN
		UPDATE SET {{ extractMergeUpdate(targetDataSet,'q') }}
	WHEN NOT MATCHED BY TARGET THEN
		INSERT({{ extractColumnsCS(targetDataSet) }})
		VALUES ({{ extractMergeInsertValues(targetDataSet,'q') }})