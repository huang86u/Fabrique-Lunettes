package fr.miage.shared;

import java.util.Map;
import java.util.HashMap;

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
        Map<TypeLunette, Integer> lignes = new HashMap<>();
        if (payload == null || payload.isEmpty()) {
            return new Commande(lignes);
        }

        String[] paires = payload.split(",");
        for (String paire : paires){
            String[] elements = paire.split(":");
            if (elements.length == 2) {
                try{
                    // Convertir TypeLunette et entier
                    TypeLunette type = TypeLunette.valueOf(elements[0].trim().toUpperCase());
                    int quantite = Integer.parseInt(elements[1].trim());
                    lignes.put(type, quantite);
                } catch (IllegalArgumentException e) {
                    // A voir si je met un message d'erreur ou si je laisse tomber la ligne mal formée
                }
            }
        }
        return new Commande(lignes);
    }
}
