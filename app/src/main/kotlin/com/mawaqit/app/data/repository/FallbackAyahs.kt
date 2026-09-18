package com.mawaqit.app.data.repository

/**
 * Curated fallback ayahs (ASSETS.md §6) — used when no cached ayah exists and,
 * for now (PHASE_4), as the daily rotation source itself. PHASE_6 adds the
 * UmmahAPI fetch; this list stays as the last-resort fallback.
 * Rotation: index = dayOfMonth % list.size.
 */
data class FallbackAyah(
    val arabic: String,
    val english: String,
    val urdu: String,
    val reference: String
)

val FALLBACK_AYAHS = listOf(
    FallbackAyah(
        arabic = "ٱللَّهُ لَآ إِلَٰهَ إِلَّا هُوَ ٱلْحَىُّ ٱلْقَيُّومُ",
        english = "Allah — there is no deity except Him, the Ever-Living, the Self-Sustaining.",
        urdu = "اللہ کے سوا کوئی معبود نہیں وہ زندہ ہے سب کو تھامے ہوئے ہے۔",
        reference = "Surah Al-Baqarah, 2:255"
    ),
    FallbackAyah(
        arabic = "إِنَّ مَعَ ٱلْعُسْرِ يُسْرًا",
        english = "Indeed, with hardship will be ease.",
        urdu = "یقیناً مشکل کے ساتھ آسانی ہے۔",
        reference = "Surah Ash-Sharh, 94:6"
    ),
    FallbackAyah(
        arabic = "وَمَن يَتَوَكَّلْ عَلَى ٱللَّهِ فَهُوَ حَسْبُهُۥ",
        english = "And whoever relies upon Allah — then He is sufficient for him.",
        urdu = "جو اللہ پر بھروسہ کرے تو وہ اس کے لیے کافی ہے۔",
        reference = "Surah At-Talaq, 65:3"
    ),
    FallbackAyah(
        arabic = "فَٱذْكُرُونِىٓ أَذْكُرْكُمْ",
        english = "So remember Me; I will remember you.",
        urdu = "مجھے یاد کرو میں تمہیں یاد کروں گا۔",
        reference = "Surah Al-Baqarah, 2:152"
    ),
    FallbackAyah(
        arabic = "وَلَذِكْرُ ٱللَّهِ أَكْبَرُ",
        english = "And the remembrance of Allah is greater.",
        urdu = "اللہ کا ذکر سب سے بڑا ہے۔",
        reference = "Surah Al-Ankabut, 29:45"
    ),
    FallbackAyah(
        arabic = "إِنَّ ٱللَّهَ مَعَ ٱلصَّـٰبِرِينَ",
        english = "Indeed, Allah is with the patient.",
        urdu = "بے شک اللہ صبر کرنے والوں کے ساتھ ہے۔",
        reference = "Surah Al-Baqarah, 2:153"
    ),
    FallbackAyah(
        arabic = "وَهُوَ مَعَكُمْ أَيْنَ مَا كُنتُمْ",
        english = "And He is with you wherever you are.",
        urdu = "اور وہ تمہارے ساتھ ہے جہاں بھی تم ہو۔",
        reference = "Surah Al-Hadid, 57:4"
    ),
    FallbackAyah(
        arabic = "رَبَّنَآ ءَاتِنَا فِى ٱلدُّنْيَا حَسَنَةً وَفِى ٱلْءَاخِرَةِ حَسَنَةً",
        english = "Our Lord, give us in this world good and in the Hereafter good.",
        urdu = "اے ہمارے رب ہمیں دنیا میں بھی بھلائی دے اور آخرت میں بھی۔",
        reference = "Surah Al-Baqarah, 2:201"
    ),
    FallbackAyah(
        arabic = "حَسْبُنَا ٱللَّهُ وَنِعْمَ ٱلْوَكِيلُ",
        english = "Sufficient for us is Allah, and He is the best Disposer of affairs.",
        urdu = "ہمیں اللہ کافی ہے اور وہ بہترین کارساز ہے۔",
        reference = "Surah Al-Imran, 3:173"
    ),
    FallbackAyah(
        arabic = "إِنَّ ٱلصَّلَوٰةَ كَانَتْ عَلَى ٱلْمُؤْمِنِينَ كِتَـٰبًا مَّوْقُوتًا",
        english = "Indeed, prayer has been decreed upon the believers a decree of specified times.",
        urdu = "نماز مومنوں پر مقررہ اوقات میں فرض ہے۔",
        reference = "Surah An-Nisa, 4:103"
    )
)
