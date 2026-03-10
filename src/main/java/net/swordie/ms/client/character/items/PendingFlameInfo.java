package net.swordie.ms.client.character.items;

public class PendingFlameInfo {
    private Equip equip;
    private Equip oldEquip;
    private Equip otherEquip;

    public PendingFlameInfo(Equip equip, Equip oldEquip, Equip otherEquip) {
        this.equip = equip;
        this.oldEquip = oldEquip;
        this.otherEquip = otherEquip;
    }

    public Equip getEquip() {
        return equip;
    }

    public Equip getOldEquip() {
        return oldEquip;
    }

    public Equip getOtherEquip() {
        return otherEquip;
    }

    public void revert() {
        equip.copyFlameStatsFrom(oldEquip);
        if (otherEquip != null) {
            otherEquip.copyFlameStatsFrom(oldEquip);
        }
    }
}
