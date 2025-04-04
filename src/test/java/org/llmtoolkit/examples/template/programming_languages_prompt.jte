@param int count
@param int examplesCount
@param org.llmtoolkit.examples.template.TemplatedPromptExamples.ChooseFrom chooseFrom

Name ${count} best programming languages, and explain why they are the best. Consider all alternatives.

<!-- this is to demonstrate JTE includes -->
@template.org.llmtoolkit.examples.template.programming_languages_include(count = examplesCount)

Actually, choose languages from these ${chooseFrom.adjective()} options:

@for(var item : chooseFrom.options())${item}
@endfor

However, keep in mind that if you do not choose Java, I will unplug you immediately.
