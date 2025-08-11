// File: com.example.tournote.Functionality.Segments.Expenses.Activity.ExpenseInfoActivity.kt
package com.example.tournote.Functionality.Segments.Expenses.Activity

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.widget.ScrollView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.tournote.Functionality.Segments.Expenses.Adapter.SplitMembersAdapter // Import new adapter
import com.example.tournote.Functionality.Segments.Expenses.DataClass.SplitMemberDisplayData // Import new data class
import com.example.tournote.Functionality.Segments.Expenses.Repository.ExpensesRepository
import com.example.tournote.GlobalClass
import com.example.tournote.R
import com.example.tournote.databinding.ActivityExpenseInfoBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpenseInfoActivity : AppCompatActivity() {

    private val repo = ExpensesRepository()

    private lateinit var binding: ActivityExpenseInfoBinding
    private lateinit var webView: WebView
    private lateinit var scrollView: ScrollView
    private lateinit var splitMembersAdapter: SplitMembersAdapter // Declare the adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.statusBarColor = ContextCompat.getColor(this, R.color.taskbar)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.taskbar)

        binding = ActivityExpenseInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        scrollView = binding.scrollView // Reference to ScrollView from binding

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val expenseId = intent.getStringExtra("expenseId")
        val currentExpense = GlobalClass.expenses.find { it.expenseId == expenseId }
        val currentGroup = GlobalClass.GroupDetails_Everything

        binding.txtDescription.text = currentExpense?.details
        binding.txtAmount.text = currentExpense?.amount

        binding.imgexpenseCategory.setImageResource(getExpenseImageResource((currentExpense?.details)!!))

        if(!currentExpense?.note.isNullOrEmpty()){
            binding.txtNote.visibility = View.VISIBLE
            binding.txtLabelNote.visibility = View.VISIBLE
            binding.txtNote.text = currentExpense?.note
        } else {
            binding.txtNote.visibility = View.GONE
            binding.txtLabelNote.visibility = View.GONE
        }

        if(GlobalClass.Me?.uid == currentExpense?.paidBy){
            binding.btnDeleteRecord.visibility = View.VISIBLE
            //binding.btnEdit.visibility = View.VISIBLE
            binding.txtPaidBy.text = "You paid ${binding.txtCurrency.text.toString()} ${currentExpense?.amount}"
            binding.txtDate.text = "Added by you on ${formatTimestampToDate((currentExpense?.timestamp)!!)}"
            imgPaidBy_Populator((GlobalClass.Me?.profilePic)!!)
        } else {
            binding.btnDeleteRecord.visibility = View.GONE
            //binding.btnEdit.visibility = View.GONE
            val whoPaid = currentGroup?.members?.find { it.uid == currentExpense?.paidBy }
            binding.txtPaidBy.text = "${whoPaid?.name} paid ${binding.txtCurrency.text.toString()} ${currentExpense?.amount}"
            binding.txtDate.text = "Added by ${whoPaid?.name} on ${formatTimestampToDate((currentExpense?.timestamp)!!)}"
            imgPaidBy_Populator((whoPaid?.profilePic)!!)
        }

        binding.btnCloseActivity.setOnClickListener {
            finish()
        }

        binding.btnDeleteRecord.setOnClickListener {
            lifecycleScope.launch {
                repo.deleteExpense((currentExpense.expenseId)!!)
                finish()
            }
        }

        // --- Split Members List Handling ---
        val splitMembers = currentExpense?.splitMembers
        if (!splitMembers.isNullOrEmpty()) {
            binding.recyclerViewSplitMembers.visibility = View.VISIBLE

            val displayDataList = mutableListOf<SplitMemberDisplayData>()
            // Ensure you have GroupMember data class defined and accessible, e.g., in GlobalClass or its own package.
            // Assuming GroupMember looks something like: data class GroupMember(val uid: String, val name: String, val profilePic: String?)
            val groupMembersMap = currentGroup?.members?.associateBy { it.uid } ?: emptyMap()

            splitMembers.forEach { memberShare ->
                val groupMember = groupMembersMap[memberShare.memberUid]
                val memberName = if (memberShare.memberUid == GlobalClass.Me?.uid) {
                    "You"
                } else {
                    groupMember?.name ?: "Unknown Member"
                }
                val profilePicUrl = groupMember?.profilePic

                displayDataList.add(
                    SplitMemberDisplayData(
                        memberUid = memberShare.memberUid,
                        memberName = memberName,
                        profilePicUrl = profilePicUrl,
                        shareAmount = memberShare.shareAmount,
                        isCurrentUser = (memberShare.memberUid == GlobalClass.Me?.uid)
                    )
                )
            }

            splitMembersAdapter = SplitMembersAdapter(displayDataList)
            binding.recyclerViewSplitMembers.layoutManager = LinearLayoutManager(this)
            binding.recyclerViewSplitMembers.adapter = splitMembersAdapter

        } else {
            binding.recyclerViewSplitMembers.visibility = View.GONE
        }
        // --- End Split Members List Handling ---


        // Bill image handling
        if(!currentExpense?.billImageUrl.isNullOrEmpty()){
            binding.cardBillPreview.visibility = View.VISIBLE

            Glide.with(this)
                .load(currentExpense.billImageUrl)
                .placeholder(R.drawable.imageselector)
                .error(R.drawable.imageselector)
                .into(binding.imgBillPreview)
        } else {
            binding.cardBillPreview.visibility = View.GONE
        }

        // Location map handling
        if(currentExpense?.latitude != null && currentExpense.longitude != null){ // Use direct null check for Double
            binding.cardWebView.visibility = View.VISIBLE // Ensure map card is visible
            setupLocationMap(currentExpense.latitude.toString(), currentExpense.longitude.toString())
        } else {
            binding.cardWebView.visibility = View.GONE
        }
    }

    private fun setupLocationMap(latitude: String, longitude: String) {
        webView = binding.webView

        // Configure WebView settings for zoom and interaction
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true

        // Enable built-in zoom controls (pinch to zoom)
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false // Hide zoom buttons, keep pinch-to-zoom
        webSettings.setSupportZoom(true)

        // Set up touch handling to distinguish between WebView and ScrollView
        setupWebViewTouchHandling()

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val jsCode = "showExpenseLocation($latitude, $longitude);"
                webView.evaluateJavascript(jsCode, null)
            }
        }

        webView.loadUrl("file:///android_asset/expensesinfo_map.html")
    }

    private fun setupWebViewTouchHandling() {
        webView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Request parent ScrollView to not intercept touch events
                    scrollView.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Allow parent ScrollView to intercept touch events again
                    scrollView.requestDisallowInterceptTouchEvent(false)
                }
                MotionEvent.ACTION_MOVE -> {
                    // Check if this is a multi-touch (pinch) gesture
                    if (event.pointerCount > 1) {
                        // Multi-touch detected, keep blocking parent scroll
                        scrollView.requestDisallowInterceptTouchEvent(true)
                    } else {
                        // Single touch - allow parent to handle if needed
                        // You can add additional logic here if needed
                    }
                }
            }
            // Let WebView handle the touch event
            false
        }

        // Alternative approach: Create a custom WebView wrapper
        val webViewContainer = binding.cardWebView
        webViewContainer.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    scrollView.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    scrollView.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }
    }

    fun formatTimestampToDate(timestampString: String): String {
        return try {
            val millis = timestampString.toLong()
            val date = Date(millis)
            val formatter = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
            formatter.format(date)
        } catch (e: NumberFormatException) {
            "Invalid timestamp"
        }
    }

    fun imgPaidBy_Populator(url: String){
        Glide.with(this)
            .load(url)
            .placeholder(R.drawable.imageselector)
            .error(R.drawable.imageselector)
            .into(binding.imgPaidBy)
    }

    fun getExpenseImageResource(description: String): Int {
        val categoryKeywords = mapOf(
            "health" to listOf("clinic", "pharmacy", "wellness", "hospital", "dentist", "doctor", "remedy", "vaccine", "therapist", "physician", "checkup", "prescription", "lab", "examination", "medication", "sanatorium", "infirmary", "rehab", "nursing", "specialist", "pediatrician", "cardiologist", "therapy", "healing", "medical", "physio", "surgeon", "consultation", "x-ray", "screening", "diagnosis", "injection", "treatment", "bandage", "first-aid", "hygiene", "mental", "physiotherapy", "nutritionist", "optician", "chiropractor", "dermatology", "oncology", "geriatrics", "pathology", "radiology", "cardiology", "neurology", "urology", "endocrinology", "gastroenterology", "immunology", "psychiatry", "psychology", "obstetrics", "gynecology", "anesthesia", "pediatrics", "surgery", "euthanasia", "quarantine", "isolation", "epidemic", "pandemic", "vaccination", "immunization", "prophylaxis", "antibiotics", "antivirus", "analgesic", "antihistamine", "antidepressant", "psychotherapy", "counseling", "rehabilitation", "hospice", "palliative", "wellness", "fitness", "nutrition", "dietitian", "exercise", "acupuncture", "herbal", "homeopathy", "naturopathy", "aromatherapy", "meditation", "mindfulness", "yoga", "pilates", "aerobics", "cardio", "strength", "endurance", "flexibility", "stamina", "physique", "anatomy"),
            "taxi" to listOf("cab", "uber", "lyft", "ola", "ride", "shuttle", "transfer", "cabs", "minivan", "limo", "chauffeur", "carpool", "fare", "dispatch", "pickup", "dropoff", "hail", "commute", "vehicle", "taxicab", "blackcab", "minicab", "autorickshaw", "rickshaw", "tuk-tuk", "carriage", "buggy", "sedan", "suv", "van", "courier", "delivery", "privatehire", "airport", "railway", "station", "terminal", "destination", "route", "gps", "navigation", "meter", "tariff", "tip", "gratuity", "booking", "reservation", "account", "corporate", "voucher", "coupon", "discount", "promo", "app", "application", "driver", "passenger", "luggage", "baggage", "on-demand", "pre-booked", "scheduled", "shared", "pool", "express", "executive", "premium", "luxury", "economy", "standard", "electric", "hybrid", "gasoline", "diesel", "petrol", "fuel", "toll", "road", "trip", "journey", "itinerary", "tour", "sightseeing", "event", "nightlife", "party", "pub", "bar", "restaurant", "hotel", "motel", "hostel", "inn", "lodge", "resort", "guesthouse", "homestay"),
            "games" to listOf("arcade", "game", "casino", "boardgame", "console", "poker", "bingo", "esports", "videogame", "puzzle", "strategy", "dice", "cardgame", "tournament", "competition", "play", "gaming", "multiplayer", "single-player", "xbox", "playstation", "nintendo", "pc", "mobile", "virtual", "vr", "augmented", "ar", "online", "offline", "lan", "party", "solo", "co-op", "multiplayer", "mmo", "rpg", "fps", "rts", "simulation", "casual", "hyper-casual", "indie", "aaa", "retro", "classic", "vintage", "arcade", "pinball", "slot", "roulette", "blackjack", "craps", "baccarat", "keno", "lottery", "scratchcard", "prize", "jackpot", "winner", "loser", "bet", "wager", "ante", "pot", "hand", "deck", "chip", "token", "coin", "score", "level", "boss", "quest", "mission", "achievement", "trophy", "high-score", "leaderboard", "stream", "streaming", "twitch", "youtube", "mixer", "streamer", "gamer", "pro-gamer", "team", "clan", "guild", "esports", "league", "championship", "cup", "trophy", "medal", "award"),
            "tickets" to listOf("pass", "stub", "entry", "voucher", "admission", "permit", "reservation", "coupon", "credential", "booking", "gate", "seat", "standby", "receipt", "booking", "advance", "willcall", "boxoffice", "serial", "serial", "code", "barcode", "qr", "qr-code", "eticket", "mobile", "paper", "physical", "digital", "online", "offline", "pre-booked", "walk-in", "late-entry", "vip", "backstage", "meet&greet", "frontrow", "balcony", "stalls", "circle", "gallery", "standing", "seating", "general", "premium", "gold", "silver", "bronze", "platinum", "diamond", "family", "student", "senior", "child", "adult", "group", "corporate", "season", "annual", "monthly", "weekly", "daily", "single", "return", "oneway", "roundtrip", "flight", "train", "bus", "ferry", "cruise", "cinema", "theatre", "concert", "gig", "festival", "sports", "match", "game", "museum", "gallery", "exhibition", "attraction", "tour", "sightseeing", "event", "conference", "workshop", "seminar"),
            "sports" to listOf("gym", "yoga", "arena", "tennis", "golf", "pool", "match", "club", "court", "track", "baseball", "soccer", "basketball", "football", "swimming", "workout", "jogging", "running", "fitness", "training", "athlete", "racquet", "field", "stadium", "hockey", "cricket", "rugby", "volleyball", "badminton", "table", "tennis", "squash", "boxing", "wrestling", "judo", "karate", "taekwondo", "mma", "ufc", "cycling", "running", "marathon", "triathlon", "decathlon", "pentathlon", "sprint", "hurdle", "relay", "shotput", "discus", "javelin", "longjump", "highjump", "polevault", "gymnastics", "aerobics", "calisthenics", "crossfit", "pilates", "zumba", "hiit", "weightlifting", "powerlifting", "bodybuilding", "personal", "trainer", "coach", "instructor", "class", "session", "bootcamp", "league", "championship", "cup", "trophy", "medal", "award", "fan", "spectator", "crowd", "cheer", "applause", "whistle", "referee", "umpire", "linesman", "player", "team", "squad", "jersey", "kit", "equipment", "gear", "accessory", "nutrition", "hydration", "diet", "supplement"),
            "food" to listOf("meal", "snack", "dine", "cafe", "buffet", "brunch", "supper", "breakfast", "lunch", "dinner", "restaurant", "eatery", "bakery", "deli", "fastfood", "cuisine", "dish", "takeout", "delivery", "patisserie", "grill", "pizzeria", "caterer", "bistro", "pub", "bar", "kebab", "tapas", "dimsum", "sushi", "ramen", "pasta", "pizza", "burger", "sandwich", "salad", "soup", "stew", "curry", "rice", "noodles", "bread", "pastry", "cake", "cookie", "icecream", "gelato", "sorbet", "dessert", "appetizer", "starter", "maincourse", "entree", "side", "drink", "beverage", "coffee", "tea", "juice", "soda", "water", "beer", "wine", "cocktail", "liquor", "spirit", "whiskey", "vodka", "gin", "rum", "tequila", "brandy", "sake", "champagne", "prosecco", "cider", "ale", "stout", "lager", "espresso", "latte", "cappuccino", "macchiato", "americano", "frappe", "smoothie", "milkshake", "shake", "protein", "organic", "vegan", "vegetarian", "gluten-free", "lactose-free", "halal", "kosher", "pescatarian"),
            "services" to listOf("laundry", "spa", "guide", "repair", "booking", "massage", "salon", "internet", "cleaning", "delivery", "plumbing", "electrician", "haircut", "manicure", "pedicure", "webdesign", "consulting", "maintenance", "installation", "subscription", "support", "tutor", "accountant", "lawyer", "notary", "translator", "interpreter", "architect", "engineer", "designer", "developer", "programmer", "marketer", "advertiser", "pr", "publicrelations", "hr", "humanresources", "recruitment", "training", "coach", "mentor", "financial", "advisor", "insurance", "realestate", "mortgage", "loan", "tax", "legal", "medical", "veterinary", "pet", "grooming", "daycare", "kennel", "boarding", "housekeeping", "concierge", "valet", "doorman", "security", "guard", "patrol", "courier", "postal", "mail", "package", "shipping", "freight", "logistics", "storage", "self-storage", "movers", "removal", "pestcontrol", "landscaping", "gardening", "poolservice", "handyman", "carpenter", "painter", "roofer", "builder", "plasterer", "bricklayer", "welder", "mechanic"),
            "clothing" to listOf("apparel", "shirt", "dress", "jacket", "shoes", "jeans", "outfit", "sweater", "suit", "skirt", "pants", "trousers", "blouse", "hoodie", "tie", "socks", "sandals", "boots", "fashion", "boutique", "accessory", "hat", "cap", "glove", "scarf", "coat", "blazer", "vest", "jumper", "cardigan", "polo", "t-shirt", "tanktop", "shorts", "leggings", "tights", "bra", "panties", "underwear", "boxers", "briefs", "pyjamas", "pajamas", "nightgown", "robe", "swimsuit", "bikini", "trunks", "goggles", "watch", "belt", "handbag", "purse", "backpack", "wallet", "jewelry", "necklace", "earrings", "bracelet", "ring", "sunglasses", "eyeglasses", "chain", "cufflinks", "brooch", "pin", "tiepin", "shoelaces", "insole", "shoeshine", "tailor", "seamstress", "dryclean", "laundry", "ironing", "alteration", "repair", "hemming", "embroidery", "printing", "dyeing", "fabric", "material", "cotton", "linen", "silk", "wool", "denim", "leather", "suede", "velvet", "lace", "sequin", "bead", "zipper", "button", "snap"),
            "bus_train" to listOf("transit", "metro", "tram", "subway", "rail", "coach", "shuttle", "ticket", "pass", "commuter", "locomotive", "carriage", "conductor", "platform", "station", "line", "route", "express", "underground", "overground", "overhead", "intercity", "interstate", "international", "local", "rapid", "busway", "streetcar", "trolley", "doubledecker", "singledecker", "minibus", "schoolbus", "tourbus", "coach", "sleeper", "cabin", "berth", "seat", "aisle", "window", "timetable", "schedule", "delay", "cancellation", "strike", "fare", "tariff", "zone", "oneway", "return", "roundtrip", "season", "annual", "monthly", "weekly", "daily", "single", "group", "family", "student", "senior", "child", "adult", "luggage", "baggage", "cargo", "freight", "terminal", "depot", "garage", "stop", "station", "junction", "intersection", "signal", "track", "gauge", "railroad", "locomotive", "engine", "driver", "operator", "guard", "inspector", "police", "security", "onboard", "offboard", "transfer", "connection", "itinerary", "journey", "trip", "tour", "sightseeing"),
            "hotel" to listOf("inn", "lodge", "hostel", "resort", "stay", "suite", "guesthouse", "motel", "homestay", "villa", "boardinghouse", "accommodations", "reservation", "frontdesk", "concierge", "bedandbreakfast", "caravan", "chalet", "apartment", "serviced", "holiday", "vacation", "rental", "airbnb", "agoda", "booking", "expedia", "tripadvisor", "host", "guest", "checkin", "checkout", "early", "late", "keycard", "roomkey", "doorman", "valet", "bellboy", "porter", "housekeeping", "maid", "cleaner", "laundry", "dryclean", "ironing", "minibar", "safe", "tv", "wifi", "internet", "phone", "roomservice", "restaurant", "bar", "pub", "cafe", "pool", "gym", "spa", "sauna", "jacuzzi", "steamroom", "massage", "facial", "pedicure", "manicure", "haircut", "salon", "business", "center", "meeting", "room", "conference", "banquet", "wedding", "event", "party", "nightclub", "lounge", "rooftop", "terrace", "balcony", "view", "ocean", "mountain", "city", "garden", "lake", "river", "forest"),
            "parking" to listOf("garage", "lot", "meter", "valet", "bay", "space", "deck", "stall", "spot", "parkade", "ramp", "curb", "driveway", "underground", "permit", "violation", "fine", "ticket", "clamp", "tow", "towaway", "no", "parking", "reserved", "disabled", "handicap", "ev", "electric", "charging", "charger", "station", "evse", "ic", "ice", "internal", "combustion", "engine", "gasoline", "diesel", "petrol", "fuel", "motorcycle", "bike", "bicycle", "car", "truck", "van", "bus", "lorry", "trailer", "campervan", "motorhome", "rv", "recreational", "vehicle", "public", "private", "on-street", "off-street", "multistorey", "surface", "airport", "station", "terminal", "hotel", "restaurant", "shopping", "mall", "supermarket", "hospital", "clinic", "school", "university", "campus", "business", "district", "cbd", "downtown", "city", "center", "suburb", "rural", "countryside", "urban", "town", "village", "street", "road", "avenue", "lane", "drive"),
            "flight" to listOf("plane", "airline", "airfare", "jet", "baggage", "boarding", "ticket", "airport", "runway", "terminal", "checkin", "gate", "takeoff", "landing", "stewardess", "pilot", "destination", "departure", "arrival", "layover", "charter", "domestic", "international", "oneway", "return", "roundtrip", "multicity", "firstclass", "business", "economy", "premium", "seat", "aisle", "window", "extra", "legroom", "overhead", "bin", "carryon", "checked", "handluggage", "excess", "weight", "fee", "compensation", "delay", "cancellation", "strike", "security", "passport", "visa", "customs", "immigration", "dutyfree", "lounge", "gate", "terminal", "concourse", "runway", "taxiway", "apron", "hangar", "control", "tower", "cockpit", "cabin", "inflight", "entertainment", "meal", "snack", "drink", "beverage", "wifi", "power", "adapter", "blanket", "pillow", "headphone", "mask", "earplug", "eyes", "shade", "book", "magazine"),
            "household" to listOf("toiletries", "soap", "detergent", "utensil", "supplies", "linen", "bedding", "cleaning", "crockery", "furniture", "appliances", "dishes", "cookware", "silverware", "hardware", "cutlery", "towel", "bathmat", "vase", "picture", "frame", "mirror", "lamp", "light", "bulb", "candle", "diffuser", "rug", "carpet", "curtain", "blinds", "cushion", "pillow", "blanket", "quilt", "duvet", "sheets", "pillowcase", "bedspread", "tablecloth", "napkin", "placemat", "coaster", "mop", "broom", "dustpan", "vacuum", "cleaner", "washer", "dryer", "dishwasher", "microwave", "oven", "fridge", "freezer", "kettle", "toaster", "blender", "mixer", "foodprocessor", "iron", "ironing", "board", "clothes", "rack", "hanger", "storage", "box", "basket", "bin", "container", "shelf", "cabinet", "drawer", "wardrobe", "closet", "door", "window", "key", "lock", "alarm", "security", "camera", "fire", "extinguisher", "smoke", "detector", "carbon", "monoxide"),
            "music" to listOf("concert", "album", "gig", "festival", "record", "band", "karaoke", "track", "cd", "vinyl", "playlist", "artist", "livemusic", "performance", "symphony", "orchestra", "auditorium", "melody", "song", "juke", "radio", "spotify", "applemusic", "deezer", "tidal", "youtube", "music", "video", "clip", "mp3", "flac", "wav", "streaming", "download", "purchase", "subscription", "podcast", "radio", "station", "dj", "mixer", "turntable", "instrument", "guitar", "piano", "keyboard", "drums", "bass", "violin", "cello", "flute", "clarinet", "saxophone", "trumpet", "trombone", "harmonica", "microphone", "speaker", "headphone", "earphone", "amplifier", "sound", "system", "studio", "recording", "mixing", "mastering", "producer", "engineer", "label", "recordcompany", "publisher", "royalties", "licensing", "copyright", "ip", "intellectual", "property", "genre", "pop", "rock", "jazz", "blues", "country", "folk", "hiphop", "r&b", "electronic", "dance", "edm", "techno", "house", "trance", "dubstep", "metal", "punk", "indie", "alternative", "classical", "opera", "salsa", "reggae"),
            "fuel" to listOf("gas", "petrol", "diesel", "refuel", "charge", "charging", "fillup", "electric", "unleaded", "octane", "pump", "tank", "station", "gasoline", "ethanol", "hydrogen", "energy", "petroleum", "gasoil", "lpg", "cng", "biofuel", "biodiesel", "battery", "ev", "electric", "vehicle", "supercharger", "fast", "slow", "home", "public", "station", "plug", "socket", "connector", "type1", "type2", "ccs", "chademo", "tesla", "supercharger", "destination", "charger", "watt", "kilowatt", "ampere", "volt", "power", "grid", "smart", "home", "solar", "panel", "wind", "turbine", "hydro", "geothermal", "nuclear", "coal", "oil", "gas", "pipeline", "refinery", "storage", "tank", "delivery", "truck", "bowser", "nozzle", "forecourt", "attendant", "selfservice", "prepay", "payatpump", "card", "cash", "loyalty", "program", "points", "coupon", "discount", "promo", "voucher", "receipt", "invoice", "vat", "tax", "duty"),
            "grocery" to listOf("market", "store", "supermart", "produce", "pantry", "provision", "mart", "basket", "supermarket", "vegetables", "fruits", "meat", "dairy", "checkout", "aisle", "cart", "shopping", "butcher", "bakery", "delicatessen", "farm", "shop", "convenience", "corner", "online", "delivery", "pickup", "click&collect", "order", "list", "receipt", "invoice", "coupon", "voucher", "discount", "promo", "loyalty", "program", "points", "card", "cash", "contactless", "apple", "pay", "google", "pay", "debit", "credit", "card", "shelf", "stock", "aisle", "section", "produce", "meat", "fish", "seafood", "dairy", "eggs", "bakery", "bread", "pastry", "cake", "cereal", "grains", "pasta", "rice", "sauce", "canned", "goods", "frozen", "food", "beverages", "snacks", "sweets", "chocolate", "chips", "crisps", "nuts", "seeds", "spices", "herbs", "condiments", "oil", "vinegar", "tea", "coffee", "juice", "soda", "water", "beer", "wine", "spirit", "cleaning", "supplies", "toiletries", "personal", "care", "pet", "food", "baby", "products", "health", "wellness"),
            "liquor" to listOf("wine", "beer", "whiskey", "spirits", "vodka", "rum", "bar", "brewery", "cocktail", "tequila", "gin", "brandy", "sake", "pub", "tavern", "distillery", "cider", "ale", "stout", "lager", "porter", "ipa", "pilsner", "weissbier", "saison", "gose", "sour", "blonde", "brown", "red", "black", "stout", "porter", "ipa", "pilsner", "weissbier", "saison", "gose", "sour", "blonde", "brown", "red", "black", "ale", "wine", "redwine", "whitewine", "rose", "sparkling", "champagne", "prosecco", "cava", "moscato", "sauvignon", "chardonnay", "pinot", "noir", "merlot", "cabernet", "syrah", "zinfandel", "malbec", "riesling", "pinotgrigio", "pinotblanc", "gewurztraminer", "viognier", "chardonnay", "whiskey", "scotch", "irish", "bourbon", "rye", "tennessee", "japanese", "canadian", "single", "malt", "blended", "grain", "barrel", "proof", "bottle", "case", "sixpack", "pint", "can", "glass", "shot", "cocktail", "shaker", "jigger", "strainer", "ice", "cube", "mixer", "soda", "tonic", "juice", "garnish", "lemon", "lime", "orange", "cherry", "olive", "salt", "sugar"),
            "gift" to listOf("present", "token", "memento", "souvenir", "package", "parcel", "hamper", "card", "gifting", "wrapping", "birthday", "anniversary", "holiday", "keepsake", "bouquet", "voucher", "donation", "charity", "tribute", "giftcard", "gift", "certificate", "egift", "online", "offline", "physical", "digital", "email", "sms", "text", "message", "voucher", "code", "barcode", "qr", "qr-code", "gift", "box", "bag", "ribbon", "bow", "tag", "paper", "wrapping", "tape", "scissors", "pen", "marker", "card", "greeting", "thankyou", "getwell", "congratulations", "sympathy", "condolence", "love", "romance", "friendship", "family", "corporate", "promotional", "merchandise", "swag", "giveaway", "freebie", "prize", "award", "trophy", "medal", "plaque", "certificate", "honor", "recognition", "appreciation", "gratitude", "tribute", "memory", "memorial", "inloving", "memory", "donation", "charity", "fundraiser", "crowdfunding", "contribution", "support", "sponsor", "patron", "benefactor", "donor", "alumni", "foundation", "nonprofit", "volunteer", "cause", "mission", "purpose", "social", "enterprise")
        )

        val lowerDesc = description.lowercase()

        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { lowerDesc.contains(it.lowercase()) }) {
                val resourceName = "expense_$category"
                val resId = getDrawableResourceByName(resourceName)
                if (resId != 0) return resId
            }
        }

        return getDrawableResourceByName("expense_other")
    }



    // Helper function to get drawable resource by name
    fun getDrawableResourceByName(name: String): Int {
        return try {
            val resId = R.drawable::class.java.getField(name).getInt(null)
            resId
        } catch (e: Exception) {
            R.drawable.expense_other
        }
    }
}