package adamantpenguin.bookletx;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;

public class BlooketGame {
    private final int gameId;
    private String username = "";
    private String blook = "Fox"; // default value in case something goes wrong setting it later
    private String fbToken = "";
    private Long balance = 0L;
    private String stg = "join";
    private final DatabaseReference db;
    private FirebaseAuth fbAuth;

    public enum Glitch {
        JOKESTER, LUNCH_BREAK, NIGHT_TIME, AD_SPAM
    }

    /**
     * Blooket game interaction helper class thing.
     * Use it to talk to Blooket live games without fuddling with Firebase.
     * @param context requireContext() or something
     * @param gameId Game ID of the live game
     */
    public BlooketGame(Context context, int gameId) {
        this.gameId = gameId;
        // set up Firebase if it isn't already
        try {FirebaseApp.getInstance();} catch (IllegalStateException e) {
            String[] urls = {
                    "https://blooket-2020.firebaseio.com",
                    "https://blooket-2021.firebaseio.com",
                    "https://blooket-2022.firebaseio.com",
                    "https://blooket-2023.firebaseio.com",
                    "https://blooket-2024.firebaseio.com",
                    "https://blooket-2025.firebaseio.com",
                    "https://blooket-2026.firebaseio.com",
                    "https://blooket-2027.firebaseio.com"
            };
            String correctUrl =
                    this.gameId < 212500 ? urls[0] :
                    this.gameId < 325e3 ? urls[1] :
                    this.gameId < 437500 ? urls[2] :
                    this.gameId < 55e4 ? urls[3] :
                    this.gameId < 662500 ? urls[4] :
                    this.gameId < 775e3 ? urls[5] :
                    this.gameId < 887500 ? urls[6] :
                    urls[7];
            FirebaseApp.initializeApp(context, new FirebaseOptions.Builder()
                    .setApiKey("AIzaSyCA-cTOnX19f6LFnDVVsHXya3k6ByP_MnU")
                    .setApplicationId("1:741533559105:web:b8cbb10e6123f2913519c0")
                    .setProjectId("blooket-2020")
                    .setStorageBucket("blooket-2020.appspot.com")
                    .setGcmSenderId("741533559105")
                    .setDatabaseUrl(correctUrl)
                    .build());
        }

        this.db = FirebaseDatabase.getInstance().getReference().child(String.valueOf(gameId)).getRef();

        // add event listener for stg changes
        this.db.child("stg").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                String value = (String) snapshot.getValue();
                if (value != null ) stg = value;
                if (!stg.equals("join") && !stg.equals("fin")) { // once game started, register balance updater
                    db.child("c").child(username).child("ca").addValueEventListener(balanceUpdater);
                }
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {
                Log.w("DbError", error.toException());
            }
        });
    }

    public int getGameId() { return this.gameId; }
    public String getUsername() { return this.username; }
    public void setUsername(String username) { this.username = username; }
    public String getBlook() { return this.blook; }
    public void setBlook(String blook) { this.blook = blook; }
    public void setToken(String fbToken) {
        this.fbToken = fbToken;
        this.fbAuth = FirebaseAuth.getInstance();
        this.fbAuth.signInWithCustomToken(this.fbToken);
    }

    /**
     * Authenticate with Blooket API (api.blooket.com).
     * This allows access to writing to the database - i.e. playing the game.
     * @param username Username/nickname
     */
    public void authenticate(String username, Callback callback) {
        this.username = username;
        // send request starts here
        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.parse("application/json;charset=utf-8");
        String data = String.format( // what goes to server
                Locale.ENGLISH,
                "{\"id\":\"%d\",\"name\":\"%s\"}",
                this.gameId, this.username);
        Log.d("BlAuth", data);
        RequestBody body = RequestBody.create(data, JSON);

        String userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) "
                         + "QtWebEngine/5.15.2 Chrome/83.0.4103.122 Safari/537.36"; // pretend to be a web browser
        Request request = new Request.Builder()
                .url("https://api.blooket.com/api/firebase/join")
                .addHeader("Referer", "https://www.blooket.com/")
                .addHeader("User-Agent", userAgent)
                .addHeader("Accept", "application/json, text/plain, */*")
                .addHeader("Accept-Language", "en-US,en;q=0.9")
                .put(body)
                .build();
        // use the callback from the caller
        client.newCall(request).enqueue(callback);
    }

    public void enterGame() {
        HashMap<String, String> data = new HashMap<>();
        data.put("b", this.blook);
        this.db.child("c").child(this.username).setValue(data);
    }
    public void exitGame() {
        this.db.child("c").child(this.username).removeValue();
    }

    public void setBalance(int amount) {
        this.db.child("c").child(this.username).child("ca").setValue(amount);
    }
    ValueEventListener balanceUpdater = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
            try {
                balance = (Long) snapshot.getValue();
            } catch (NullPointerException ignored) {}
        }

        @Override
        public void onCancelled(@NonNull @NotNull DatabaseError error) {
            Log.w("DbError", error.toException());
        }
    };
    public Long getBalance() { return this.balance; }

    public void doGlitch(Glitch glitch) {
        String glitchCode = "";
        switch (glitch) {
            case JOKESTER: glitchCode = "j"; break;
            case LUNCH_BREAK: glitchCode = "lb"; break;
            case NIGHT_TIME: glitchCode = "nt"; break;
            case AD_SPAM: glitchCode = "as"; break;
        }
        this.db.child("c").child(username).child("tat").setValue(glitchCode);
    }
    // TODO add other methods such as setBalance
}
