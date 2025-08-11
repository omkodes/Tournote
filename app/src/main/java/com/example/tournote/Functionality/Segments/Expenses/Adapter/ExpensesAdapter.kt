package com.example.tournote.Functionality.Segments.Expenses.Adapter

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tournote.Functionality.Segments.Expenses.Activity.ExpenseInfoActivity
import com.example.tournote.Functionality.Segments.Expenses.SealedClass.ExpenseListItem
import com.example.tournote.Functionality.Segments.Expenses.DataClass.ExpensesDataClass
import com.example.tournote.GlobalClass
import com.example.tournote.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Constants to differentiate view types
private const val ITEM_TYPE_EXPENSE = 0
private const val ITEM_TYPE_HEADER = 1

class ExpensesAdapter : ListAdapter<ExpenseListItem, RecyclerView.ViewHolder>(ExpenseListItemDiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        // Determine the type of item at this position
        return when (getItem(position)) {
            is ExpenseListItem.ExpenseItem -> ITEM_TYPE_EXPENSE
            is ExpenseListItem.MonthHeader -> ITEM_TYPE_HEADER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // Inflate the correct layout and return the appropriate ViewHolder based on viewType
        return when (viewType) {
            ITEM_TYPE_EXPENSE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_expense, parent, false)

                ExpenseViewHolder(view)
            }
            ITEM_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_month_header, parent, false)
                MonthHeaderViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    // Existing ViewHolder for individual expense items
    inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtMonth: TextView = itemView.findViewById(R.id.txtMonth)
        private val txtDate: TextView = itemView.findViewById(R.id.txtDate)
        private val txtDescription: TextView = itemView.findViewById(R.id.txtDescription)
        private val txtWhoPaidToWhom: TextView = itemView.findViewById(R.id.txtWhoPaidToWhom)
        val txtStatus: TextView = itemView.findViewById(R.id.txtStatus)
        val txtAmount: TextView = itemView.findViewById(R.id.txtAmount)
        val txtWhoPaid : TextView = itemView.findViewById(R.id.txtWhoPaidToWhom)
        val imgexpenseCategory : ImageView = itemView.findViewById(R.id.imgexpenseCategory)

        val body : ConstraintLayout = itemView.findViewById(R.id.itemBody)

        fun bind(expense: ExpensesDataClass) {
            val timestampLong = expense.timestamp.toLongOrNull()

            if (timestampLong != null) {
                try {
                    val date = Date(timestampLong)
                    val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                    val dayFormat = SimpleDateFormat("dd", Locale.getDefault())

                    txtMonth.text = monthFormat.format(date)
                    txtDate.text = dayFormat.format(date)
                } catch (e: Exception) {
                    txtMonth.text = ""
                    txtDate.text = ""
                    Log.e("ExpensesAdapter", "Error creating Date object from timestamp $timestampLong: ${e.message}")
                }
            } else {
                txtMonth.text = ""
                txtDate.text = ""
                Log.e("ExpensesAdapter", "Timestamp '${expense.timestamp}' is not a valid Long.")
            }

            txtDescription.text = expense.details
            imgexpenseCategory.setImageResource(getExpenseImageResource(expense.details))
            val paidByUid = expense.paidBy
            val currentGroup = GlobalClass.GroupDetails_Everything

            if (GlobalClass.Me?.uid == paidByUid) {
                txtWhoPaidToWhom.text = "You paid ₹${expense.amount}"
            } else {
                val payerName = currentGroup?.members?.find { it.uid == paidByUid }?.name
                txtWhoPaidToWhom.text = "${payerName ?: "Someone"} paid ₹${expense.amount}"
            }

        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            ITEM_TYPE_EXPENSE -> {
                val context = holder.itemView.context // ✅ correct context
                val expenseItem = getItem(position) as ExpenseListItem.ExpenseItem
                val expense = expenseItem.expense
                (holder as ExpenseViewHolder).bind(expense)

                holder.body.setOnClickListener {
                    val intent = Intent(context, ExpenseInfoActivity::class.java)
                    intent.putExtra("expenseId", expense.expenseId) // ✅ pass the ID
                    context.startActivity(intent)
                }

                if(GlobalClass.Me?.uid==expense.paidBy){
                    holder.txtStatus.text="you lent"
                    holder.txtStatus.setTextColor(ContextCompat.getColor(context, R.color.textGreen))
                    holder.txtAmount.setTextColor(ContextCompat.getColor(context, R.color.textGreen))


                    if((expense.splitType=="SELF")||(expense.splitType=="null")){
                        holder.txtStatus.setTextColor(ContextCompat.getColor(context, R.color.black))
                        holder.txtAmount.visibility=View.GONE
                        holder.txtStatus.text="No balence"
                    }else{
                        var total = 0.00
                        for(member in (expense.splitMembers)!!){
                            if((member.memberUid!= GlobalClass.Me?.uid)&&(member.paid==false)){
                                total+=member.shareAmount.toDouble()
                            }
                        }
                        holder.txtAmount.text = "₹" + String.format("%.2f", total)
                    }

                }else{
                    if((expense.splitMembers?.find { it.memberUid == GlobalClass.Me?.uid } ==null)||(expense.splitMembers?.find { it.memberUid == GlobalClass.Me?.uid }?.paid ==true)){
                        holder.txtStatus.setTextColor(ContextCompat.getColor(context, R.color.black))
                        holder.txtAmount.visibility=View.GONE
                        holder.txtStatus.text="No balence"
                    }
                    else{
                        holder.txtStatus.setTextColor(ContextCompat.getColor(context, R.color.textRed))
                        holder.txtAmount.setTextColor(ContextCompat.getColor(context, R.color.textRed))
                        holder.txtStatus.text="you borrowed"
                        holder.txtAmount.text="₹"+ String.format("%.2f", (expense.splitMembers?.find { it.memberUid == GlobalClass.Me?.uid }?.shareAmount))
                    }
                }
            }

            ITEM_TYPE_HEADER -> {
                val headerItem = getItem(position) as ExpenseListItem.MonthHeader
                (holder as MonthHeaderViewHolder).bind(headerItem.monthYear)
            }
        }
    }



    // NEW: ViewHolder for the Month/Year Header
    inner class MonthHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtMonthYearHeader: TextView = itemView.findViewById(R.id.txtMonthYearHeader)

        fun bind(monthYear: String) {
            txtMonthYearHeader.text = monthYear
        }
    }

    // DiffUtil for ExpenseListItem, now handling both types
    class ExpenseListItemDiffCallback : DiffUtil.ItemCallback<ExpenseListItem>() {
        override fun areItemsTheSame(oldItem: ExpenseListItem, newItem: ExpenseListItem): Boolean {
            return when {
                oldItem is ExpenseListItem.ExpenseItem && newItem is ExpenseListItem.ExpenseItem ->
                    // Assuming timestamp and details together are unique for an expense
                    oldItem.expense.timestamp == newItem.expense.timestamp && oldItem.expense.details == newItem.expense.details
                oldItem is ExpenseListItem.MonthHeader && newItem is ExpenseListItem.MonthHeader ->
                    oldItem.monthYear == newItem.monthYear
                else -> false // Different types are never the same item
            }
        }

        override fun areContentsTheSame(oldItem: ExpenseListItem, newItem: ExpenseListItem): Boolean {
            // Data classes' 'equals' method handles content comparison for ExpenseItem and MonthHeader
            return oldItem == newItem
        }
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