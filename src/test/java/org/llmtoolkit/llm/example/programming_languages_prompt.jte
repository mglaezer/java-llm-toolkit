@param int count
@param int examplesCount
@param org.llmtoolkit.llm.example.ProgrammingLanguagesPrompt.ChooseFrom chooseFrom

Name ${count} best programming languages, and explain why they are the best. Consider all alternatives.

<!-- this is to demonstrate JTE includes -->
@template.org.llmtoolkit.llm.example.programming_languages_include(count = examplesCount)

Actually, choose languages from these ${chooseFrom.adjective()} options:

@for(var item : chooseFrom.options())${item}
@endfor