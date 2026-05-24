package fr.miage.shared;

import java.util.EnumMap;
import java.util.Map;

public class FormatteurMessage {
    // Encode la commande pour MQTT
    // Exemple sortie : "BANANA:2,CLAUDE:1"
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

    // Decode le message MQTT en Commande
    // Exemple d'entrée : "BANANA:2,CLAUDE:1"
    public static Commande decoder(String payload){
        Map<TypeLunette, Integer> lignes = new EnumMap<>(TypeLunette.class);
        if (payload == null || payload.isEmpty()) {
            return new Commande(lignes);
        }

        String[] paires = payload.split(",", -1);
        for (String paire : paires){
            String[] elements = paire.split(":", -1);
            if (elements.length != 2 || elements[0].isBlank() || elements[1].isBlank()) {
                throw new IllegalArgumentException("format de commande invalide");
            }

            TypeLunette type;
            try {
                type = TypeLunette.valueOf(elements[0].trim().toUpperCase());
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("type de lunette inconnu : " + elements[0].trim(), exception);
            }

            try {
                lignes.put(type, Integer.parseInt(elements[1].trim()));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("quantite invalide pour " + type.name(), exception);
            }
        }
        return new Commande(lignes);
    }
}
