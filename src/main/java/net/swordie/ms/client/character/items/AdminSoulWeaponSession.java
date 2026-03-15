package net.swordie.ms.client.character.items;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.soulcollection.BossSoulType;
import net.swordie.ms.client.soulcollection.SoulCollectionModule;
import net.swordie.ms.connection.packet.ZeroPool;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.constants.SoulCollectionConstants;
import net.swordie.ms.loaders.StringData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AdminSoulWeaponSession {
    private final Equip equip;
    private final short equipPos;
    private final List<BossSoulType> soulTypes = new ArrayList<>();
    private BossSoulType selectedSoulType;

    public AdminSoulWeaponSession(Equip equip, short equipPos) {
        this.equip = equip;
        this.equipPos = equipPos;
        for (BossSoulType soulType : BossSoulType.values()) {
            soulTypes.add(soulType);
        }
        soulTypes.sort(Comparator.comparing(this::getDisplayName, String.CASE_INSENSITIVE_ORDER));
    }

    public boolean isComplete() {
        return selectedSoulType != null;
    }

    public boolean isValid() {
        return ItemConstants.isWeapon(equip.getItemId()) && !soulTypes.isEmpty();
    }

    public boolean select(int index) {
        if (index < 0 || index >= soulTypes.size()) {
            return false;
        }
        selectedSoulType = soulTypes.get(index);
        return true;
    }

    public String getSelectionText() {
        StringBuilder sb = new StringBuilder();
        sb.append("#e<Admin Soul Weapon Picker>#n\r\n");
        sb.append("Select a soul weapon for #v").append(equip.getItemId()).append("##t")
                .append(equip.getItemId()).append("# (inventory id: ").append(equip.getBagIndexWithEquipped())
                .append(").\r\n\r\n#b");
        for (int i = 0; i < soulTypes.size(); i++) {
            sb.append("#L").append(i).append("#").append(getDisplayName(soulTypes.get(i))).append("#l\r\n");
        }
        sb.append("#k");
        return sb.toString();
    }

    public String apply(Char chr) {
        if (selectedSoulType == null) {
            return "No soul weapon selected.";
        }
        int previousSoulOptionId = equip.getSoulOptionId();
        int soulOptionId = SoulCollectionConstants.getRepresentativeSoulId(selectedSoulType);
        equip.setSoulSocketId((short) 1);
        equip.setSoulOptionId((short) soulOptionId);
        equip.setSoulOption((short) ItemConstants.getRandomSoulOption());
        equip.updateToChar(chr);

        if (equipPos < 0) {
            if (previousSoulOptionId > 0) {
                int oldSkillId = SoulCollectionConstants.getSoulSkillFromSoulID(previousSoulOptionId);
                if (oldSkillId > 0 && chr.hasSkill(oldSkillId)) {
                    chr.removeSkillAndSendPacket(oldSkillId);
                }
            }
            int newSkillId = SoulCollectionConstants.getSoulSkillFromSoulID(soulOptionId);
            if (newSkillId > 0) {
                int soulSkillLv = SoulCollectionModule.getSoulSkillLevelBySoulType(chr, selectedSoulType);
                chr.addSkill(newSkillId, Math.max(1, soulSkillLv), 2);
            }
        }

        if (JobConstants.isZero(chr.getJob()) && ItemConstants.isLongOrBigSword(equip.getItemId())) {
            int otherEquipPos = Math.abs(equipPos) == 10 ? 11 : 10;
            Equip otherEquip = (Equip) chr.getEquippedInventory().getItemBySlot(otherEquipPos);
            if (otherEquip != null) {
                otherEquip.copySoulOptionsFrom(equip);
                otherEquip.updateToChar(chr);
                chr.write(ZeroPool.egoEquipComplete(true, true));
            }
        }
        return String.format("Applied %s soul weapon to #v%d##t%d#.", getDisplayName(selectedSoulType), equip.getItemId(), equip.getItemId());
    }

    private String getDisplayName(BossSoulType soulType) {
        String name = soulType.name().replaceAll("([a-z])([A-Z])", "$1 $2");
        if (name.equals("Captain Darkgoo")) {
            String itemName = StringData.getItemStringById(2591250);
            return itemName != null ? itemName.replace(" Soul", "") : name;
        }
        return name;
    }
}
