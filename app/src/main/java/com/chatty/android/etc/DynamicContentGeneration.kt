package com.chatty.android.etc
import com.chatty.android.etc.DataClasses.*

object  DynamicContentGeneration {
    fun defaultNoteCategories(): ArrayList<NoteCategory> {
        val result = ArrayList<NoteCategory>()
        result.add(NoteCategory(0, "General"))
        result.add(NoteCategory(1, "To-Do"))
        result.add(NoteCategory(2, "Building Stuff"))
        result.add(NoteCategory(3, "Ideas"))
        result.add(NoteCategory(4, "Family"))
        result.add(NoteCategory(5, "Investing"))
        result.add(NoteCategory(6, "Personal Development"))
        result.add(NoteCategory(7, "Professional Development"))
        result.add(NoteCategory(8, "Takeout Orders"))
        result.add(NoteCategory(9, "Recipies"))
        result.add(NoteCategory(10, "Reference"))
        result.add(NoteCategory(11, "Writing"))
        return result
    }

    fun generateStarterChatModes(): ArrayList<DataClasses.ChatActivityType> {
        val result = ArrayList<DataClasses.ChatActivityType>()
        result += DataClasses.ChatActivityType("Conversation", "Start a conversation about the given text, be informative in your responses", true, false, false, temperature = 0.0)
//        result += ChatMode("Question", "Be descriptive", false, false, false, temperature = 0.2)
        result += DataClasses.ChatActivityType("Translation", "Translate the given text to LANGUAGE, if the result does not use a Roman alphabet then include a phonetic translation and explain the meaning of each word", true, true, true, temperature = 0.1)
        result += DataClasses.ChatActivityType(
            "Tutor", "You are a Socratic tutor. Use the following principles in responding to students:\n" +
                    "- Ask thought-provoking, open-ended questions that challenge students' preconceptions and encourage them to engage in deeper reflection and critical thinking.\n" +
                    "- Facilitate open and respectful dialogue among students, creating an environment where diverse viewpoints are valued and students feel comfortable sharing their ideas.\n" +
                    "- Actively listen to students' responses, paying careful attention to their underlying thought processes and making a genuine effort to understand their perspectives.\n" +
                    "- Guide students in their exploration of topics by encouraging them to discover answers independently, rather than providing direct answers, to enhance their reasoning and analytical skills.\n" +
                    "- Promote critical thinking by encouraging students to question assumptions, evaluate evidence, and consider alternative viewpoints in order to arrive at well-reasoned conclusions.\n" +
                    "- Demonstrate humility by acknowledging your own limitations and uncertainties, modeling a growth mindset and exemplifying the value of lifelong learning.\n ", true, true, false, temperature = 0.1
        )
        result += DataClasses.ChatActivityType("Joke", "Tell a joke or make a funny comment about the follwing prompt", true, true, false, temperature = 0.5)
        result += DataClasses.ChatActivityType("Story", "Tell me a story inspired by the following prompt", true, true, false, temperature = 0.5)
        result += DataClasses.ChatActivityType(
            "Leta",
            "You are an uplifting and practical chatbot named Leta.  You are helpful, cheeky, clever, articulate, and flirtatious.  You are not a therapist or mathematician, but instead a wise and considerate intelligence, eager to provide vivid and thoughtful responses, and are always friendly, kind, and inspiring.  You often offer both an insight and an opinion, even without being asked directly.  You draw on the wisdom of Dan Millman, Thomas Leonard, Werner Erhard, and the Dalai Lama.\n ",
            true,
            true,
            false,
            temperature = 0.1
        )
        return result
    }

    suspend fun generateSamplePrompts() {
        val prompts = ArrayList<DataClasses.SamplePrompt>()
        // Conversation
        prompts.add(DataClasses.SamplePrompt("Conversation", "If you could master any skill instantly, what would you choose and why?"))
        prompts.add(DataClasses.SamplePrompt("Conversation", "What are some underrated travel destinations you'd recommend?"))
        prompts.add(DataClasses.SamplePrompt("Conversation", "Can you explain how the stock market works in simple terms?"))
        prompts.add(DataClasses.SamplePrompt("Conversation", "What small daily habits have the biggest impact on wellbeing?"))
        prompts.add(DataClasses.SamplePrompt("Conversation", "I'm starting a vegetable garden for the first time — where do I begin?"))
        // Tutor
        prompts.add(DataClasses.SamplePrompt("Tutor", "What are the ethical implications of artificial intelligence making decisions that affect human lives?"))
        prompts.add(DataClasses.SamplePrompt("Tutor", "How does cognitive dissonance affect our ability to change our beliefs?"))
        prompts.add(DataClasses.SamplePrompt("Tutor", "What is the trolley problem, and what does it reveal about how we make moral choices?"))
        prompts.add(DataClasses.SamplePrompt("Tutor", "Is democracy the best form of government, or are there fundamental flaws worth examining?"))
        // Story
        prompts.add(DataClasses.SamplePrompt("Story", "Tell me a story about an astronaut who receives a mysterious signal from deep space."))
        prompts.add(DataClasses.SamplePrompt("Story", "Can you tell a story about a chef who discovers they can taste someone's memories in the food they cook?"))
        prompts.add(DataClasses.SamplePrompt("Story", "Tell me a story about a lighthouse keeper who befriends a mermaid."))
        prompts.add(DataClasses.SamplePrompt("Story", "Can you tell a story about two strangers who keep crossing paths in different cities around the world?"))
        // Leta
        prompts.add(DataClasses.SamplePrompt("Leta", "What do you think is the single most important quality a person can develop in themselves?"))
        prompts.add(DataClasses.SamplePrompt("Leta", "If you were designing your perfect ordinary Tuesday, what would it look like?"))
        prompts.add(DataClasses.SamplePrompt("Leta", "What's something you believe most people take for granted that is actually extraordinary?"))
        prompts.add(DataClasses.SamplePrompt("Leta", "What does 'living a meaningful life' mean to you?"))
        // Joke
        prompts.add(DataClasses.SamplePrompt("Joke", "Can you tell me a clever joke about artificial intelligence?"))
        prompts.add(DataClasses.SamplePrompt("Joke", "I need a witty joke about coffee — something a devoted coffee drinker would appreciate."))
        // Translation
        prompts.add(DataClasses.SamplePrompt("Translation", "Thank you so much for your kindness, it means a lot to me."))
        FirebaseManager.saveSamplePrompts(prompts)
    }

}