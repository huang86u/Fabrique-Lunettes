package fr.miage.backend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bernard_flou.Fabricateur;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;

class SuiviCommandeTest {
    
    @Test
    @SuppressWarnings("unchecked")
    void suiviCommandeNeDevientTermineeQueQuandToutesLesQuantitesSontAtteintes() throws Exception {
        Class<?> suiviClass = Class.forName("fr.miage.backend.MqttServer$SuiviCommande");
        Constructor<?> constructor = suiviClass.getDeclaredConstructor(int.class);
        constructor.setAccessible(true);
        
        Object suivi = constructor.newInstance(2);
        
        Method estTerminee = suiviClass.getDeclaredMethod("estTerminee");
        estTerminee.setAccessible(true);
        
        Field champLunettes = suiviClass.getDeclaredField("lunettesFabriquees");
        champLunettes.setAccessible(true);
        List<Fabricateur.Lunette> lunettesFabriquees = (List<Fabricateur.Lunette>) champLunettes.get(suivi);
        
        assertFalse((boolean) estTerminee.invoke(suivi), "La commande ne doit pas être terminée au début (0/2)");
        
        lunettesFabriquees.add(new Fabricateur.Lunette(Fabricateur.TypeLunette.BANANA, "SN-TEST-001"));
        assertFalse((boolean) estTerminee.invoke(suivi), "La commande ne doit pas être terminée s'il manque des lunettes (1/2)");
        
        lunettesFabriquees.add(new Fabricateur.Lunette(Fabricateur.TypeLunette.CLAUDE, "SN-TEST-002"));
        assertTrue((boolean) estTerminee.invoke(suivi), "La commande DOIT être terminée car les 2 lunettes sont fabriquées (2/2)");
    }
}