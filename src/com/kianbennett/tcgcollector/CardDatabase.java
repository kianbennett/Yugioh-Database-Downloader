package com.kianbennett.tcgcollector;

import com.google.gson.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Pattern;

public class CardDatabase {

    public class CardDatabaseJson {
        public Date date;
        public Object[] cards;
        public CardDatabaseJson(Date date, Object[] cards) {
            this.date = date;
            this.cards = cards;
        }
    }

    public Date dateCreated;
    public List<Card> cardList; // Dictionary of cards using the card id as a key

    private Gson gson;
    private JsonParser parser;

    public CardDatabase(Date dateCreated, boolean min, boolean basic, String out, boolean img) {
        this.dateCreated = dateCreated;
        cardList = new ArrayList<>();
        if(min) {
            gson = new Gson();
        } else {
            gson = new GsonBuilder().setPrettyPrinting().create();
        }

        parser = new JsonParser();

        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(1);

        if(img) new File("Card Images").mkdir();

        try {
            getCardList();

            System.out.println(cardList.size() + " cards to load...");

            int interval = 50;
            if(img) interval = 10;

            if(!basic) {
                int cardsLoaded = 0;
                for(int i = 0; i < (int) ((float) cardList.size() / interval) + 1; i++) {
                    List<Card> cards = new ArrayList<>();
                    for(int c = i * interval; c < i * interval + interval; c++) {
                        if(c < cardList.size()) {
                            cards.add(getCardFromIndex(c));
                        }
                    }
                    if(cards.size() > 0) {
                        long timeStart = new Date().getTime();
                        getCardDetails(cards);
                        getCardImageUrls(cards, img);
                        getCardTips(cards);
                        cardsLoaded += cards.size();
                        long timeFinished = new Date().getTime();
                        System.out.println("Loaded " + cardsLoaded + "/" + cardList.size() + " cards [" + df.format((float) (timeFinished - timeStart) / 1000f) + "s]");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Collections.sort(cardList);

        String json = writeJson();

        System.out.println("Writing to " + out);
        File file = new File(out);
        try {
            FileUtils.writeStringToFile(file, json, "UTF-8");
        } catch(IOException e) {
            e.printStackTrace();
        }

        Date dateFinished = new Date();
        long timeElapsed = dateFinished.getTime() - dateCreated.getTime();
        System.out.println("Done in " + df.format((float) timeElapsed / 1000f) + "s");
    }

    private void getCardList() throws IOException {
        String url = "http://yugioh.wikia.com/api/v1/Articles/List?category=TCG_cards&limit=99999&namespaces=0";
        String json = IOUtils.toString(new URL(url), "utf-8");
        JsonObject jsonObject = parser.parse(json).getAsJsonObject();
        JsonArray items = jsonObject.getAsJsonArray("items");

        for(int i = 0; i < items.size(); i++) {
            int id = items.get(i).getAsJsonObject().get("id").getAsInt();
            String title = items.get(i).getAsJsonObject().get("title").getAsString();
            String wikiUrl = items.get(i).getAsJsonObject().get("url").getAsString();
            if(!title.contains("(temp)")) {
                cardList.add(new Card(id, title, wikiUrl));
            }
        }
    }

    private void getCardDetails(List<Card> cards) throws IOException {
        String url = "http://yugioh.wikia.com/api.php?format=json&action=query&pageids=";
        for(int i = 0; i < cards.size(); i++) {
            if(i != 0) url += "|";
            url += cards.get(i).id;
        }
        url += "&prop=revisions&rvprop=content";

        String json = IOUtils.toString(new URL(url), "utf-8");
//        String json = readFileFromURL(url);

        JsonObject jsonObj = parser.parse(json).getAsJsonObject();
        JsonObject pages = jsonObj.getAsJsonObject("query").getAsJsonObject("pages");

        for(int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            JsonObject page = pages.getAsJsonObject(Integer.toString(card.id));
            String wikiText = page.getAsJsonArray("revisions").get(0).getAsJsonObject().get("*").getAsString();

            WikitextObject wikitextObject = new WikitextObject(wikiText);
            WikitextObject.Table table = wikitextObject.getTable("CardTable2");
            if(table == null) continue;

            if(table.hasProperty("lore")) card.lore = table.getProperty("lore").value;
            if(table.hasProperty("image")) card.image = table.getProperty("image").value;
            if(table.hasProperty("attribute")) card.attribute = table.getProperty("attribute").value;

            if(table.hasProperty("archseries")) {
                String[] array = listPropertyToStrings(table.getProperty("archseries"));
                if(array.length > 0) card.archetypes = array;
            }
            if(table.hasProperty("related_to_archseries")) {
                String[] array = listPropertyToStrings(table.getProperty("related_to_archseries"));
                if(array.length > 0) card.archetypesRelated = array;
            }
            if(table.hasProperty("supports_archetypes")) {
                String[] array = listPropertyToStrings(table.getProperty("supports_archetypes"));
                if(array.length > 0) card.archetypesSupports = array;
            }
            if(table.hasProperty("action")) {
                String[] array = listPropertyToStrings(table.getProperty("action"));
                if(array.length > 0) card.actions = array;
            }

            if(table.hasProperty("level")) card.level = table.getProperty("level").value;
            if(table.hasProperty("atk")) card.atk = table.getProperty("atk").value;
            if(table.hasProperty("def")) card.def = table.getProperty("def").value;
            if(table.hasProperty("number")) {
                if(!table.getProperty("number").value.isEmpty()) card.number = table.getProperty("number").value.replaceAll("\\D+","");
            }

            List<String> listTypes = new ArrayList<>();
            if(table.hasProperty("type")) listTypes.add(table.getProperty("type").value);
            if(table.hasProperty("typest")) listTypes.add(table.getProperty("typest").value);
            if(table.hasProperty("type1")) listTypes.add(table.getProperty("type1").value);
            if(table.hasProperty("type2")) listTypes.add(table.getProperty("type2").value);
            if(table.hasProperty("type3")) listTypes.add(table.getProperty("type3").value);
            if(listTypes.size() > 0) card.types = listTypes.toArray(new String[] {});

            if(table.hasProperty("fm")) card.fm = table.getProperty("fm").value;
            if(table.hasProperty("effect_types")) {
                card.effectTypes = table.getProperty("effect_types").value.split(",");
                for(int e = 0; e < card.effectTypes.length; e++) card.effectTypes[e] = card.effectTypes[e].trim();
            }
            if(table.hasProperty("materials")) {
                card.materials = table.getProperty("materials").value.split(Pattern.quote("+"));
                for(int m = 0; m < card.materials.length; m++) {
                    card.materials[m] = card.materials[m].trim();
//                    card.materials[m] = card.materials[m].replace("&quot;", "");
                }
            }

            if(table.hasProperty("pendulum_effect")) card.pendulumEffect = table.getProperty("pendulum_effect").value;
            if(table.hasProperty("pendulum_effect_types")) {
                card.pendulumEffectTypes = table.getProperty("pendulum_effect_types").value.split(",");
                for(int e = 0; e < card.pendulumEffectTypes.length; e++) card.pendulumEffectTypes[e] = card.pendulumEffectTypes[e].trim();
            }

            if(table.hasProperty("ocg")) card.statusOcg = table.getProperty("ocg").value;
            if(table.hasProperty("adv")) card.statusTcgAdv = table.getProperty("adv").value;
            if(table.hasProperty("trad")) card.statusTcgTrad = table.getProperty("trad").value;

            if(table.hasProperty("en_sets") || table.hasProperty("na_sets") || table.hasProperty("eu_sets")) {
                List<Card.CardSet> cardSets = new ArrayList<>();
                cardSets.addAll(getCardSets(table, "en_sets", ""));
                cardSets.addAll(getCardSets(table, "na_sets", " (NA)"));
                cardSets.addAll(getCardSets(table, "eu_sets", " (EU)"));
                if(cardSets.size() > 0) card.sets = cardSets;
            }
        }
    }

    private void getCardImageUrls(List<Card> cards, boolean download) throws IOException {
        String url = "http://yugioh.wikia.com/api.php?format=json&action=query&titles=";
        for(int i = 0; i < cards.size(); i++) {
            if(i != 0) url += "|";
            url += "File:" + cards.get(i).image;
        }
        url += "&prop=imageinfo&iiprop=url";

        String json = IOUtils.toString(new URL(url), "utf-8");
        JsonObject jsonObj = parser.parse(json).getAsJsonObject();
        JsonObject pages = jsonObj.getAsJsonObject("query").getAsJsonObject("pages");

        Set<Map.Entry<String, JsonElement>> entries = pages.entrySet();
        for (Map.Entry<String, JsonElement> entry : entries) {
            JsonObject page = entry.getValue().getAsJsonObject();
            String pageTitle = page.get("title").getAsString();
            for(int i = 0; i < cards.size(); i++) {
                if(pageTitle.equals("File:" + cards.get(i).image)) {
                    if(page.get("imageinfo") == null) continue;
                    cards.get(i).imageUrl = page.get("imageinfo").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
                    if(cards.get(i).imageUrl.contains(".png")) {
                        cards.get(i).imageUrl = cards.get(i).imageUrl.substring(0, cards.get(i).imageUrl.lastIndexOf(".png")) + ".png";
                    }
                    if(cards.get(i).imageUrl.contains(".jpg")) {
                        cards.get(i).imageUrl = cards.get(i).imageUrl.substring(0, cards.get(i).imageUrl.lastIndexOf(".jpg")) + ".jpg";
                    }

                    if(download) {
                        URL imgUrl = new URL(cards.get(i).imageUrl);
                        try {
                            InputStream in = new BufferedInputStream(imgUrl.openStream());
                            OutputStream out = new BufferedOutputStream(new FileOutputStream("Card Images/" + cards.get(i).image));

                            for (int r; (r = in.read()) != -1;) {
                                out.write(r);
                            }
                            in.close();
                            out.close();
                        } catch(Exception e) {
                            System.out.println("Unable to reach image file for '" + cards.get(i).title + "' (" + e.getMessage() + ")");
                        }
                    }

                    break;
                }
            }
        }
    }

    private void getCardTips(List<Card> cards) throws IOException {
        String url = "http://yugioh.wikia.com/api.php?format=json&action=query&titles=";
        for(int i = 0; i < cards.size(); i++) {
            if(i != 0) url += "|";
            url += "Card_Tips:" + cards.get(i).wikiUrl.replace("/wiki/", "");
        }
        url += "&prop=revisions&rvprop=content";

        String json = IOUtils.toString(new URL(url), "utf-8");
//        String json = readFileFromURL(url);

        JsonObject jsonObj = parser.parse(json).getAsJsonObject();
        JsonObject pages = jsonObj.getAsJsonObject("query").getAsJsonObject("pages");

        Set<Map.Entry<String, JsonElement>> entries = pages.entrySet();//will return members of your object
        for (Map.Entry<String, JsonElement> entry : entries) {
            JsonObject page = entry.getValue().getAsJsonObject();
            if(page.get("revisions") == null) continue;
            String pageTitle = page.get("title").getAsString();
            for(int i = 0; i < cards.size(); i++) {
                Card card = cards.get(i);
                if(pageTitle.equals("Card Tips:" + card.title)) {
                    String wikiText = page.get("revisions").getAsJsonArray().get(0).getAsJsonObject().get("*").getAsString();
                    WikitextObject wikitextObject = new WikitextObject(wikiText);
                    List<Card.PropertyListValue> tipsStandard = new ArrayList<>(), tipsTraditional = new ArrayList<>();
                    for(int l = 0; l < wikitextObject.lists.size(); l++) {
                        WikitextObject.ListProperty list = wikitextObject.lists.get(l);
                        if(list.heading == null) {
                            tipsStandard.add(listPropertyToPropertyListValue(list));
                        } else if(list.heading.equals("Traditional Format")) {
                            tipsTraditional.add(listPropertyToPropertyListValue(list));
                        }
                    }
                    if(tipsStandard.size() > 0) card.tips = tipsStandard;
                    if(tipsTraditional.size() > 0) card.tipsTraditional = tipsTraditional;
                    break;
                }
            }
        }
    }

    private List<Card.CardSet> getCardSets(WikitextObject.Table table, String name, String setNameSuffix) {
        List<Card.CardSet> cardSets = new ArrayList<>();

        if(table.hasProperty(name)) {
            for(int s = 0; s < table.getProperty(name).tables.size(); s++) {
                WikitextObject.Table setTable = table.getProperty(name).tables.get(s);
                if(setTable.name.equals("Card table set")) {
                    Card.CardSet cardSet = new Card.CardSet(null, null, null);
                    if(setTable.properties.size() >= 1) cardSet.number = setTable.properties.get(0).key;
                    if(setTable.properties.size() >= 2) cardSet.setName = setTable.properties.get(1).key + setNameSuffix;
                    if(setTable.properties.size() >= 3) cardSet.rarity = setTable.properties.get(2).key;
                    cardSets.add(cardSet);
                }
                if(setTable.name.equals("")) {
                    Card.CardSet cardSet = new Card.CardSet(null, null, null);
                    if(setTable.properties.size() >= 1) cardSet.number = setTable.properties.get(1).key;
                    if(setTable.properties.size() >= 2) cardSet.setName = setTable.properties.get(0).key + setNameSuffix;
                    if(setTable.properties.size() >= 3) cardSet.rarity = setTable.properties.get(2).key;
                    cardSets.add(cardSet);
                }
            }
        }

        return cardSets;
    }

    private String writeJson() {
        CardDatabaseJson cards = new CardDatabaseJson(dateCreated, cardList.toArray());
        String json = gson.toJson(cards);

        return json;
    }

    public Card getCardFromIndex(int i) {
        return ((Card) cardList.toArray()[i]);
    }

    public Card getCardFromTitle(String title) {
        for(int i = 0; i < cardList.size(); i++) {
            Card card = getCardFromIndex(i);
            if(card.title.equals(title)) return card;
        }
        return null;
    }

    public String readFileFromURL(String url) throws IOException {
        InputStream inputStream = new URL(url).openStream();
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString("UTF-8");
    }

    private Card.PropertyListValue listPropertyToPropertyListValue(WikitextObject.ListProperty listProperty) {
        List<Card.PropertyListValue> subLists = new ArrayList<>();
        for(int s = 0; s < listProperty.subLists.size(); s++) {
            subLists.add(listPropertyToPropertyListValue(listProperty.subLists.get(s)));
        }
        if(subLists.size() == 0) subLists = null;
        Card.PropertyListValue value = new Card.PropertyListValue(listProperty.value, subLists);
        return value;
    }

    private String[] listPropertyToStrings(WikitextObject.TableProperty property) {
        List<String> list = new ArrayList<>();
        String value = property.value;
        if(value.isEmpty()) {
            for(int l = 0; l < property.lists.size(); l++) {
                list.add(property.lists.get(l).value);
            }
        } else {
            list.add(value);
        }
        return list.toArray(new String[] {});
    }
}
