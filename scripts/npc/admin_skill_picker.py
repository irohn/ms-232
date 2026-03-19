while True:
    if not session.hasSelectedSkill():
        selection = sm.sendNext(session.getSelectionText())
        if not session.select(selection):
            continue

    action = sm.sendNext(session.getActionText())
    if action == 0:
        sm.sendSayOkay(session.useSelectedSkill(chr))
        break
    if action == 1:
        key = sm.sendAskNumber("Enter the key slot index to bind this skill to.\r\n\r\nValid range: 0 - 88", 16, 0, 88)
        sm.sendSayOkay(session.bindSelectedSkill(chr, key))
        break
    session.clearSelection()
