package net.swordie.ms.client.character.items;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.field.FieldPacket;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.loaders.ItemData;

public class UniCubeSession {
    private final Equip equip;
    private final Item cubeItem;
    private final short equipPos;

    public UniCubeSession(Equip equip, Item cubeItem, short equipPos) {
        this.equip = equip;
        this.cubeItem = cubeItem;
        this.equipPos = equipPos;
    }

    public String getSelectionText() {
        StringBuilder sb = new StringBuilder();
        sb.append("#e<Uni Cube>#n\r\n");
        sb.append("Select one potential line to reroll for #v").append(equip.getItemId())
                .append("##t").append(equip.getItemId()).append("#.\r\n\r\n#b");
        int lines = equip.getOptionBase(2) == 0 ? 2 : 3;
        for (int i = 0; i < lines; i++) {
            sb.append("#L").append(i).append("#")
                    .append(i + 1).append(". ")
                    .append(getOptionDisplay(equip.getOptionBase(i)))
                    .append("#l\r\n");
        }
        sb.append("#k");
        return sb.toString();
    }

    public String apply(Char chr, int line) {
        if (line < 0 || line >= 3 || equip.getOptionBase(line) == 0) {
            return "Invalid selection.";
        }
        equip.setOptionBase(line, equip.getRandomOption(false, line));
        equip.updateToChar(chr);
        if (JobConstants.isZero(chr.getJob()) && ItemConstants.isLongOrBigSword(equip.getItemId())) {
            int otherEquipPos = Math.abs(equipPos) == 10 ? 11 : 10;
            Equip otherEquip = (Equip) chr.getEquippedInventory().getItemBySlot(otherEquipPos);
            if (otherEquip != null) {
                otherEquip.copyItemOptionsFrom(equip);
                otherEquip.updateToChar(chr);
            }
        }
        chr.write(FieldPacket.showItemReleaseEffect(chr.getId(), equipPos, false));
        chr.consumeItem(cubeItem);
        return getResultText(line);
    }

    private String getResultText(int line) {
        return "#e<Uni Cube>#n\r\nLine " + (line + 1) + " rerolled to:\r\n\r\n" + getOptionDisplay(equip.getOptionBase(line));
    }

    private String getOptionDisplay(int optionId) {
        ItemOption option = ItemData.getItemOptionById(optionId);
        return option != null ? option.getString(equip.getReqLevel()).replace("#", "") : String.valueOf(optionId);
    }
}
