@param int count
@param org.llmtoolkit.llm.example.ProgrammingLanguagesPrompt.Demo demo

Provide ${count} valid and ${count+1} lame reasons for each.

@for(var item : demo.list())${item}
@endfor