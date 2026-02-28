package net.swordie.ms.client.character.keys;

import net.swordie.ms.connection.OutPacket;
import net.swordie.orm.dao.KeymappingDao;
import net.swordie.orm.dao.SworDaoFactory;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created on 1/4/2018.
 */
@Entity
@Table(name = "funckeymap")
public class FuncKeyMap {

    private static final KeymappingDao keymappingDao = (KeymappingDao) SworDaoFactory.getByClass(Keymapping.class);
    private static final String DEFAULT_KEYMAP_TYPE_ENV = "DEFAULT_KEYMAP_TYPE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @OneToMany(
            mappedBy = "funcKeyMap",
            orphanRemoval = true,
            cascade = CascadeType.ALL
    )
    private List<Keymapping> keymap;

    @Transient
    private static final int MAX_KEYBINDS = 89;


    public FuncKeyMap(boolean init) {
        if (init) {
            keymap = new ArrayList<>();
        }
    }

    public List<Keymapping> getKeymap() {
        if (keymap == null) {
            keymap = keymappingDao.byFuncKeyMap(this);
        }
        return keymap;
    }

    public void setKeymap(ArrayList<Keymapping> keymap) {
        this.keymap = keymap;
    }

    public Keymapping getMappingAt(int index) {
        for (Keymapping km : getKeymap()) {
            if (km.getIndex() == index) {
                return km;
            }
        }
        return null;
    }

    public void encode(OutPacket outPacket, boolean encodeSize) {
        int size = getKeymap() == null ? 0 : getKeymap().size();
        if (encodeSize) {
            outPacket.encodeByte(size == 0);
        }
        if (!encodeSize || size > 0) {
            for (int i = 0; i < MAX_KEYBINDS; i++) {
                Keymapping tuple = getMappingAt(i);
                if (tuple == null) {
                    outPacket.encodeByte(0);
                    outPacket.encodeInt(0);
                } else {
                    outPacket.encodeByte(tuple.getType());
                    outPacket.encodeInt(tuple.getVal());
                }
            }
        }
    }

    public void putKeyBinding(int index, byte type, int value) {
        Keymapping km = getMappingAt(index);
        if (km == null) {
            km = new Keymapping();
            km.setIndex(index);
            km.setType(type);
            km.setVal(value);
            getKeymap().add(km);
        } else {
            km.setType(type);
            km.setVal(value);
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public static FuncKeyMap getDefaultMapping(int keySettingType) {
        FuncKeyMap fkm = new FuncKeyMap(true);
        int[] array1;
        int[] array2;
        int[] array3;
        int effectiveKeySettingType = getEffectiveKeySettingType(keySettingType);
        if (effectiveKeySettingType == 1) {
            array1 = new int[]{1, 20, 21, 22, 23, 25, 26, 27, 29, 34, 35, 36, 37, 38, 39, 40, 41, 43, 44, 45, 46, 47, 48, 49, 50, 52, 56, 57, 59, 60, 61, 63, 64, 65, 66, 70, 71, 73, 79, 82, 83};
            array2 = new int[]{4, 4, 4, 4, 4, 4, 4, 4, 5, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 4, 4, 4, 4, 4, 4, 5, 5, 6, 6, 6, 6, 6, 6, 6, 4, 4, 4, 4, 4, 4};
            array3 = new int[]{46, 27, 30, 0, 1, 19, 14, 15, 52, 17, 11, 8, 3, 20, 26, 16, 22, 9, 50, 51, 2, 31, 29, 5, 7, 4, 53, 54, 100, 101, 102, 103, 104, 105, 106, 47, 12, 13, 23, 10, 18};
        } else {
            array1 = new int[]{1, 2, 3, 4, 5, 6, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 29, 31, 34, 35, 37, 38, 39, 40, 41, 43, 44, 45, 46, 47, 48, 50, 51, 56, 57, 59, 60, 61, 63, 64, 65, 66, 70, 88};
            array2 = new int[]{4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 4, 4, 4, 4, 4, 5, 0, 0, 0, 4, 4, 4, 4, 5, 4, 0, 0, 0, 0, 0, 4, 4, 5, 5, 0, 0, 0, 0, 0, 0, 0, 4, 4};
            array3 = new int[]{46, 10, 12, 13, 18, 23, 8, 0, 0, 4, 0, 30, 2, 1, 0, 19, 27, 39, 52, 0, 17, 0, 3, 25, 11, 16, 54, 9, 0, 51, 6, 31, 0, 7, 29, 50, 53, 100, 101, 102, 103, 104, 105, 0, 47, 5};
        }
        for (int i = 0; i < array1.length; i++) {
            fkm.putKeyBinding(array1[i], (byte) array2[i], array3[i]);
        }
        return fkm;
    }

    private static int getEffectiveKeySettingType(int keySettingType) {
        String configuredType = System.getenv(DEFAULT_KEYMAP_TYPE_ENV);
        if (configuredType == null || configuredType.isBlank()) {
            return keySettingType;
        }
        try {
            int parsed = Integer.parseInt(configuredType.trim());
            if (parsed == 0 || parsed == 1) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
        }
        return keySettingType;
    }
}
