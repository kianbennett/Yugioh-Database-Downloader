package com.kianbennett.tcgcollector;

import com.oracle.webservices.internal.api.message.PropertySet;

import java.util.Comparator;
import java.util.List;

public class Card implements Comparable<Card> {

    public static class PropertyListValue {
        public String value;
        public List<PropertyListValue> subValues;
        public PropertyListValue(String value, List<PropertyListValue> subValues) {
            this.value = value;
            this.subValues = subValues;
        }
    }

    public static class CardSet {
        public String number;
        public String setName;
        public String rarity;
        public CardSet(String number, String setName, String rarity) {
            this.number = number;
            this.setName = setName;
            this.rarity = rarity;
        }
    }

    public int id;
    public String title;
    public String wikiUrl;

    public String image;
    public String lore;
    public String attribute;
    public String[] archetypes;
    public String[] archetypesRelated;
    public String[] archetypesSupports;
    public String[] actions;
    public String[] types;
    public String level;
    public String atk;
    public String def;
    public String number;
    public String fm;
    public String[] materials;
    public String[] effectTypes;
    public String[] pendulumEffectTypes;
    public String pendulumEffect;

    public String imageUrl;
    public List<PropertyListValue> tips, tipsTraditional;
    public String statusOcg, statusTcgAdv, statusTcgTrad;
    public List<CardSet> sets;

    public Card(int id, String title, String wikiUrl) {
        this.id = id;
        this.title = title;
        this.wikiUrl = wikiUrl;
    }

    public int compareTo(Card other) {
        return title.compareTo(other.title);
    }
}
