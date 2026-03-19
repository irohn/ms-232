from net.swordie.ms.loaders import StringData
from net.swordie.ms.loaders import ItemData
from net.swordie.ms.constants import ItemConstants

MAX_RESULTS = 100
VALID_CATEGORIES = {"equip", "equips", "use", "etc", "setup", "cash", "dec"}
VALID_TRADE_FILTERS = {"trade", "untrade", "transfer"}
VALID_LEVEL_FILTER_MODES = {"exact", "min", "max"}


def _safe_trim(text):
    if text is None:
        return ""
    return str(text).strip()


def _to_lower_list(values):
    out = []
    if values is None:
        return out
    for value in values:
        trimmed = _safe_trim(value)
        if trimmed != "":
            out.append(trimmed.lower())
    return out


def _matches_filters(item_name, include_terms, exclude_terms):
    lower_name = item_name.lower()
    for include_term in include_terms:
        if include_term not in lower_name:
            return False
    for exclude_term in exclude_terms:
        if exclude_term in lower_name:
            return False
    return True


def _matches_category(item_id, category):
    if category == "":
        return True
    if category == "equip":
        return ItemConstants.isEquip(item_id) and not ItemConstants.isCash(item_id)
    if category == "use":
        return ItemConstants.isConsume(item_id)
    if category == "etc":
        return ItemConstants.isEtc(item_id)
    if category == "setup":
        return ItemConstants.isInstall(item_id)
    if category == "cash":
        return ItemConstants.isCash(item_id) and not ItemConstants.isEquip(item_id)
    if category == "dec":
        return ItemConstants.isEquip(item_id) and ItemConstants.isCash(item_id)
    return True


def _matches_trade_filter(item_id, trade_filter):
    if trade_filter == "":
        return True

    item_info = ItemData.getItemInfoByID(item_id)
    if trade_filter == "transfer":
        return item_info is not None and item_info.isAccountSharable()
    if trade_filter == "trade":
        return item_info is not None and not item_info.isTradeBlock() and not item_info.isAccountSharable()
    if trade_filter == "untrade":
        if item_info is not None:
            return item_info.isTradeBlock()
        equip_info = ItemData.getEquipInfoById(item_id)
        return equip_info is not None and equip_info.isTradeBlock()
    return True


def _matches_level_filter(item_id, level_filter_mode, level_filter_value):
    if level_filter_mode == "" or level_filter_value is None:
        return True
    equip_info = ItemData.getEquipInfoById(item_id)
    if equip_info is None:
        return False
    req_level = int(equip_info.getrLevel())
    if level_filter_mode == "exact":
        return req_level == level_filter_value
    if level_filter_mode == "min":
        return req_level >= level_filter_value
    if level_filter_mode == "max":
        return req_level <= level_filter_value
    return True


def _build_display_query(query, category, trade_filter, exclude_terms, level_filter_mode, level_filter_value):
    display_query = query
    if category != "":
        display_query += " -" + category
    if trade_filter != "":
        display_query += " -" + trade_filter
    if level_filter_mode != "" and level_filter_value is not None:
        level_prefix = ""
        if level_filter_mode == "min":
            level_prefix = "+"
        elif level_filter_mode == "max":
            level_prefix = "-"
        display_query += " -lvl " + level_prefix + str(level_filter_value)
    for ex in exclude_terms:
        display_query += " !" + ex
    return display_query


query = _safe_trim(initial_query if "initial_query" in globals() else "")
exclude_terms = _to_lower_list(
    exclude_queries if "exclude_queries" in globals() else []
)
category = _safe_trim(item_category if "item_category" in globals() else "").lower()
selected_trade_filter = _safe_trim(globals()["trade_filter"] if "trade_filter" in globals() else "").lower()
selected_level_filter_mode = _safe_trim(globals()["level_filter_mode"] if "level_filter_mode" in globals() else "").lower()
selected_level_filter_value = globals()["level_filter_value"] if "level_filter_value" in globals() else None
if category == "equips":
    category = "equip"
if category not in VALID_CATEGORIES:
    category = ""
if selected_trade_filter not in VALID_TRADE_FILTERS:
    selected_trade_filter = ""
if selected_level_filter_mode not in VALID_LEVEL_FILTER_MODES:
    selected_level_filter_mode = ""
try:
    if selected_level_filter_value is not None:
        selected_level_filter_value = int(selected_level_filter_value)
except Exception:
    selected_level_filter_value = None

if query == "" and len(exclude_terms) == 0:
    query = _safe_trim(sm.sendAskText("Search items by name:", "", 1, 40))

if query == "":
    sm.sendSayOkay("Please enter at least 1 include term.")
else:
    include_terms = _to_lower_list(query.split(" "))
    result_map = StringData.getItemStringByName(query, False)
    if result_map is None or len(result_map) == 0:
        result_map = StringData.getItemStringByName(include_terms[0], False)

    if result_map is None or len(result_map) == 0:
        sm.sendSayOkay("No items were found for '#b" + query + "#k'.")
    else:
        results = []
        for item_id in result_map.keySet():
            name = result_map.get(item_id)
            if name is None:
                continue
            item_name = str(name)
            if not _matches_filters(item_name, include_terms, exclude_terms):
                continue
            try:
                iid = int(item_id)
            except Exception:
                continue
            if not _matches_category(iid, category):
                continue
            if not _matches_trade_filter(iid, selected_trade_filter):
                continue
            if not _matches_level_filter(iid, selected_level_filter_mode, selected_level_filter_value):
                continue
            results.append((iid, item_name))

        if len(results) == 0:
            display_query = _build_display_query(
                query,
                category,
                selected_trade_filter,
                exclude_terms,
                selected_level_filter_mode,
                selected_level_filter_value,
            )
            sm.sendSayOkay("No valid items were found for '#b" + display_query + "#k'.")
        else:
            results = sorted(results, key=lambda it: (it[1].lower(), it[0]))
            if len(results) > MAX_RESULTS:
                results = results[:MAX_RESULTS]

            display_query = _build_display_query(
                query,
                category,
                selected_trade_filter,
                exclude_terms,
                selected_level_filter_mode,
                selected_level_filter_value,
            )
            msg = (
                "Search results for '#b"
                + display_query
                + "#k' (showing up to "
                + str(MAX_RESULTS)
                + "):\r\n\r\n"
            )
            for i in range(len(results)):
                item_id = results[i][0]
                msg += (
                    "#L"
                    + str(i)
                    + "##v"
                    + str(item_id)
                    + "# #z"
                    + str(item_id)
                    + "##l\r\n"
                )

            sel = sm.sendNext(msg)
            if sel < 0 or sel >= len(results):
                sm.sendSayOkay("Invalid selection.")
            else:
                chosen_item_id = results[sel][0]
                chosen_item_name = results[sel][1]
                quantity = sm.sendAskNumber(
                    "Enter quantity for #v"
                    + str(chosen_item_id)
                    + "# #z"
                    + str(chosen_item_id)
                    + "#:",
                    1,
                    1,
                    30000,
                )
                if quantity <= 0:
                    sm.sendSayOkay("Invalid quantity.")
                elif not sm.canHold(chosen_item_id):
                    sm.sendSayOkay("Please make room in your inventory first.")
                else:
                    sm.giveItem(chosen_item_id, quantity)
                    sm.log.info(
                        "[{}] !s selected item {} ({}) x{}".format(
                            chr.getName(),
                            chosen_item_name,
                            chosen_item_id,
                            quantity,
                        )
                    )
                    sm.chat("Debug: selected itemID = " + str(chosen_item_id))
                    sm.sendSayOkay(
                        "You received "
                        + str(quantity)
                        + "x #v"
                        + str(chosen_item_id)
                        + "# #z"
                        + str(chosen_item_id)
                        + "#."
                    )
