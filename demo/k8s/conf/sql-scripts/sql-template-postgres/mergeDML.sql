{%set targetDataSet=dataSets[targetDataSetKeyName]%}MERGE INTO  {{ ref(targetDataSetKeyName) }} t
	USING ({{ script }}) q
	ON ({{ extractMergeOn(targetDataSet,'t','q') }})
	WHEN MATCHED THEN
		UPDATE SET {{ extractMergeUpdate(targetDataSet,'q') }}
	WHEN NOT MATCHED THEN
		INSERT({{ extractColumnsCS(targetDataSet) }})
		VALUES ({{ extractMergeInsertValues(targetDataSet,'q') }})