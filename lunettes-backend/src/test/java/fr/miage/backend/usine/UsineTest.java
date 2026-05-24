package fr.miage.backend.usine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bernard_flou.Fabricateur;
import fr.miage.shared.Commande;
import fr.miage.shared.TypeLunette;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class UsineTest {

    @Test
    void ajouterCommandeEtProduireAsynchronement() throws InterruptedException {
        Usine usine = new Usine();
        
        Map<TypeLunette, Integer> lignes = new EnumMap<>(TypeLunette.class);
        lignes.put(TypeLunette.BANANA, 3);
        lignes.put(TypeLunette.CLAUDE, 2);
        Commande commande = new Commande(lignes);

        CountDownLatch latch = new CountDownLatch(5); 
        
        AtomicInteger compteBanana = new AtomicInteger(0);
        AtomicInteger compteClaude = new AtomicInteger(0);

        usine.setOnLunetteFabriquee((orderId, lunette) -> {
            System.out.println("Usine a terminé une lunette : " + lunette.type);
            
            if ("test-order-123".equals(orderId)) {
                if (lunette.type == Fabricateur.TypeLunette.BANANA) {
                    compteBanana.incrementAndGet();
                } else if (lunette.type == Fabricateur.TypeLunette.CLAUDE) {
                    compteClaude.incrementAndGet();
                }
                latch.countDown(); 
            }
        });

        System.out.println("Envoi de la commande de 5 paires à l'usine...");
        usine.ajouterCommande("test-order-123", commande);

        //long delai
        boolean termineATemps = latch.await(45, TimeUnit.SECONDS);

        assertTrue(termineATemps, "L'usine n'a pas réussi à produire les 5 lunettes dans le temps imparti (45s)");
        assertEquals(3, compteBanana.get(), "Il devrait y avoir exactement 3 lunettes BANANA fabriquées");
        assertEquals(2, compteClaude.get(), "Il devrait y avoir exactement 2 lunettes CLAUDE fabriquées");
    }
}