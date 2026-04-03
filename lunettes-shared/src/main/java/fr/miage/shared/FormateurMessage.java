package fr.miage.shared;

import java.util.Map;
import java.lang.reflect.Type;
import java.util.HashMap;

public class FormateurMessage {
    public static String encoder(Commande commande){
        if(commande == null || commande.lignes() == null || commande.lignes().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<TypeLunette, Integer> entry : commande.lignes().entrySet()) {
            sb.append(entry.getKey().name())
            .append(":")
            .append(entry.getValue())
            .append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }
    public static Commande decoder(String payload){
        Map<TypeLunette, Integer> lignes = new HashMap<>();
        if (payload == null || payload.isEmpty()) {
            return new Commande(lignes);
        }

        String[] paires = payload.split(",");
        for (String paire : paires){
            String[] elements = paire.split(":");
            if (elements.length == 2) {
                try{
                    TypeLunette type = TypeLunette.valueOf(elements[0].trim().toUpperCase());
                    int quantite = Integer.parseInt(elements[1].trim());
                    lignes.put(type, quantite);
                } catch (IllegalArgumentException e) {
                }
            }
        }
        return new Commande(lignes);
    }
}
