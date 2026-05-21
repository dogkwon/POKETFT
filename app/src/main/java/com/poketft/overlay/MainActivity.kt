package com.poketft.overlay

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poketft.overlay.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repo.load(this)
        MyPokemonStore.load(this)

        setContent {
            PoketftTheme {
                MainScreen()
            }
        }
    }

    @Composable
    private fun MainScreen() {
        var hasPermission by remember {
            mutableStateOf(Settings.canDrawOverlays(this@MainActivity))
        }
        // 현재 화면: "main" / "register" / "edit"
        var screen by remember { mutableStateOf("main") }
        var editIndex by remember { mutableIntStateOf(-1) }

        val permLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { hasPermission = Settings.canDrawOverlays(this@MainActivity) }

        when (screen) {
            "main" -> MainPage(
                hasPermission = hasPermission,
                onRequestPermission = {
                    permLauncher.launch(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName"))
                    )
                },
                onStartOverlay = {
                    startService(Intent(this@MainActivity, OverlayService::class.java))
                    finish()
                },
                onStopOverlay = {
                    stopService(Intent(this@MainActivity, OverlayService::class.java))
                },
                onAddPokemon = { screen = "register" },
                onEditPokemon = { idx -> editIndex = idx; screen = "edit" },
                onDeletePokemon = { idx -> MyPokemonStore.remove(this@MainActivity, idx) }
            )
            "register" -> RegisterScreen(
                editSave = null,
                onSave = { save ->
                    MyPokemonStore.add(this@MainActivity, save)
                    screen = "main"
                },
                onCancel = { screen = "main" }
            )
            "edit" -> {
                val existing = MyPokemonStore.list.getOrNull(editIndex)
                RegisterScreen(
                    editSave = existing,
                    onSave = { save ->
                        MyPokemonStore.update(this@MainActivity, editIndex, save)
                        screen = "main"
                    },
                    onCancel = { screen = "main" }
                )
            }
        }
    }

    // ═══════════════════════════════════════════
    // 메인 페이지 — 오버레이 시작/종료 + 내 포켓몬 목록
    // ═══════════════════════════════════════════
    @Composable
    private fun MainPage(
        hasPermission: Boolean,
        onRequestPermission: () -> Unit,
        onStartOverlay: () -> Unit,
        onStopOverlay: () -> Unit,
        onAddPokemon: () -> Unit,
        onEditPokemon: (Int) -> Unit,
        onDeletePokemon: (Int) -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(PokeBg, PokeSurface)))
                .padding(16.dp)
        ) {
            // ── 왼쪽: 앱 정보 + 오버레이 제어 ──
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("⚔ POKETFT", color = PokeAccent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("포켓몬 실전 데미지 계산기", color = PokeTextSec, fontSize = 12.sp)
                Text("포켓몬 ${Repo.pokemons.size}종 / 기술 ${Repo.movesById.size}개",
                    color = PokeTextSec, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(20.dp))

                if (hasPermission) {
                    Button(
                        onClick = onStartOverlay,
                        colors = ButtonDefaults.buttonColors(containerColor = PokeAccent),
                        modifier = Modifier.width(180.dp)
                    ) { Text("오버레이 시작", fontWeight = FontWeight.Bold) }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(onClick = onStopOverlay, modifier = Modifier.width(180.dp)) {
                        Text("오버레이 종료", color = PokeTextSec, fontSize = 13.sp)
                    }
                } else {
                    Text("오버레이 권한 필요", color = PokeRed, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = PokeBlue)
                    ) { Text("권한 설정 열기") }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // ── 오른쪽: 내 포켓몬 등록 목록 ──
            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PokeCard)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("내 포켓몬", color = PokeTextPri, fontSize = 16.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(PokeGreen)
                            .clickable { onAddPokemon() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("+ 추가", color = Color.White, fontSize = 12.sp,
                            fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (MyPokemonStore.list.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("등록된 포켓몬이 없습니다\n'+ 추가'를 눌러 등록하세요",
                            color = PokeTextSec, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn {
                        itemsIndexed(MyPokemonStore.list) { idx, save ->
                            MyPokemonRow(save, idx, onEditPokemon, onDeletePokemon)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun MyPokemonRow(
        save: MyPokemonSave, index: Int,
        onEdit: (Int) -> Unit, onDelete: (Int) -> Unit
    ) {
        val base = MyPokemonStore.getBasePokemon(save)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PokeSurface)
                .clickable { onEdit(index) }
                .padding(8.dp)
        ) {
            // 이름 + 타입
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(base?.name_ko ?: "???", color = PokeTextPri,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    base?.types?.forEach { type ->
                        TypeBadge(type)
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                }
                // 성격 + EV 요약
                val nature = save.toNature()
                val evSummary = save.evs.zip(listOf("H","A","B","C","D","S"))
                    .filter { it.first > 0 }
                    .joinToString(" ") { "${it.second}${it.first}" }
                Text(
                    text = "${nature.nameKo} | $evSummary",
                    color = PokeTextSec, fontSize = 9.sp
                )
                // 기술
                val moveNames = save.moveIds.mapNotNull { Repo.movesById[it]?.name_ko }
                Text(
                    text = moveNames.joinToString(" / "),
                    color = PokeTextSec, fontSize = 9.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }

            // 삭제 버튼
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(PokeRed.copy(alpha = 0.6f))
                    .clickable { onDelete(index) },
                contentAlignment = Alignment.Center
            ) {
                Text("✕", color = Color.White, fontSize = 12.sp)
            }
        }
    }

    // ═══════════════════════════════════════════
    // 등록/편집 화면 — 포켓몬 선택 → 기술/노력치/성격 설정
    // ═══════════════════════════════════════════
    @Composable
    private fun RegisterScreen(
        editSave: MyPokemonSave?,
        onSave: (MyPokemonSave) -> Unit,
        onCancel: () -> Unit
    ) {
        // 상태
        var selectedPokemon by remember {
            mutableStateOf(editSave?.let { MyPokemonStore.getBasePokemon(it) })
        }
        var nature by remember {
            mutableStateOf(editSave?.toNature() ?: NatureData.NEUTRAL)
        }
        var selAbilityEn by remember { mutableStateOf(editSave?.abilityEn ?: "") }
        var selAbilityKo by remember { mutableStateOf(editSave?.abilityKo ?: "") }
        val evs = remember {
            mutableStateListOf(*(editSave?.evs?.toTypedArray() ?: arrayOf(0,0,0,0,0,0)))
        }
        val selectedMoves = remember {
            mutableStateListOf(*(editSave?.moveIds?.toTypedArray() ?: emptyArray()))
        }
        var showNaturePopup by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(PokeBg)
                .padding(12.dp)
        ) {
            // ── 왼쪽: 포켓몬 검색 ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(PokeCard)
                    .padding(8.dp)
            ) {
                Text("포켓몬 선택", color = PokeTextPri, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("이름 또는 번호", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = PokeTextPri),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PokeAccent, unfocusedBorderColor = PokeBorder,
                        cursorColor = PokeAccent
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))

                val results = remember(searchQuery) { Repo.search(searchQuery) }
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(results) { pokemon ->
                        val isSelected = selectedPokemon?.id == pokemon.id
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) PokeAccent.copy(0.3f) else Color.Transparent)
                                .clickable {
                                    selectedPokemon = pokemon
                                    selectedMoves.clear()
                                    // 첫 번째 특성 자동 선택
                                    if (pokemon.abilities.isNotEmpty()) {
                                        selAbilityEn = pokemon.abilities[0].name_en
                                        selAbilityKo = pokemon.abilities[0].name_ko
                                    } else {
                                        selAbilityEn = ""
                                        selAbilityKo = ""
                                    }
                                }
                                .padding(4.dp)
                        ) {
                            Text("#${pokemon.dex_no}", color = PokeTextSec, fontSize = 9.sp,
                                modifier = Modifier.width(30.dp))
                            Text(pokemon.name_ko, color = PokeTextPri, fontSize = 11.sp,
                                fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            pokemon.types.forEach { t ->
                                TypeBadge(t); Spacer(modifier = Modifier.width(2.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // ── 가운데: 기술 선택 ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(PokeCard)
                    .padding(8.dp)
            ) {
                Text("기술 선택 (최대 4개)", color = PokeTextPri, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))

                // 선택된 기술 표시
                if (selectedMoves.isNotEmpty()) {
                    selectedMoves.toList().forEach { moveId ->
                        val move = Repo.movesById[moveId]
                        if (move != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PokeAccent.copy(0.2f))
                                    .padding(4.dp)
                            ) {
                                TypeBadge(move.type)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(move.name_ko, color = PokeTextPri, fontSize = 10.sp,
                                    modifier = Modifier.weight(1f))
                                Text("${move.power}", color = PokeTextSec, fontSize = 10.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(PokeRed.copy(0.6f))
                                        .clickable { selectedMoves.remove(moveId) },
                                    contentAlignment = Alignment.Center
                                ) { Text("✕", color = Color.White, fontSize = 8.sp) }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = PokeBorder)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 배울 수 있는 기술 목록
                val poke = selectedPokemon
                if (poke != null) {
                    val learnableMoves = Repo.getLearnableMoves(poke)
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(learnableMoves) { move ->
                            val alreadySelected = move.id in selectedMoves
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (alreadySelected) PokeGreen.copy(0.15f) else Color.Transparent)
                                    .clickable {
                                        if (alreadySelected) {
                                            selectedMoves.remove(move.id)
                                        } else if (selectedMoves.size < 4) {
                                            selectedMoves.add(move.id)
                                        }
                                    }
                                    .padding(4.dp)
                            ) {
                                TypeBadge(move.type)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    if (move.category == "physical") "⚔" else "✦",
                                    fontSize = 8.sp, color = PokeTextSec
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(move.name_ko, color = PokeTextPri, fontSize = 10.sp,
                                    modifier = Modifier.weight(1f), maxLines = 1,
                                    overflow = TextOverflow.Ellipsis)
                                Text("${move.power}", color = PokeTextSec, fontSize = 10.sp)
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("먼저 포켓몬을 선택하세요", color = PokeTextSec, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // ── 오른쪽: 노력치 + 성격 + 저장 ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(PokeCard)
                    .padding(8.dp)
            ) {
                Text("노력치 & 성격", color = PokeTextPri, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                // 노력치 설정
                val statLabels = listOf("HP", "공격", "방어", "특공", "특방", "스피드")
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    statLabels.forEachIndexed { idx, label ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(label, color = PokeTextSec, fontSize = 10.sp,
                                modifier = Modifier.width(44.dp))
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PokeRed.copy(0.7f))
                                    .clickable {
                                        evs[idx] = (evs[idx] - 32).coerceAtLeast(0)
                                    },
                                contentAlignment = Alignment.Center
                            ) { Text("-32", color = Color.White, fontSize = 8.sp) }

                            Text("${evs[idx]}", color = PokeTextPri, fontSize = 12.sp,
                                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                                modifier = Modifier.width(36.dp))

                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PokeBlue.copy(0.7f))
                                    .clickable {
                                        evs[idx] = (evs[idx] + 32).coerceAtMost(252)
                                    },
                                contentAlignment = Alignment.Center
                            ) { Text("+32", color = Color.White, fontSize = 8.sp) }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    val total = evs.sum()
                    Text("총합: $total / 510",
                        color = if (total > 510) PokeRed else PokeTextSec,
                        fontSize = 10.sp)

                    Spacer(modifier = Modifier.height(8.dp))

                    // 성격 선택
                    Text("성격:", color = PokeTextSec, fontSize = 10.sp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(PokeYellow.copy(0.3f))
                            .clickable { showNaturePopup = true }
                            .padding(8.dp)
                    ) {
                        val desc = if (nature.isNeutral) "무보정" else
                            "↑${NatureData.STAT_LABELS[nature.upIndex-1]} ↓${NatureData.STAT_LABELS[nature.downIndex-1]}"
                        Text("${nature.nameKo} ($desc)",
                            color = PokeTextPri, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 특성 선택
                    Text("특성:", color = PokeTextSec, fontSize = 10.sp)
                    val poke2 = selectedPokemon
                    if (poke2 != null && poke2.abilities.isNotEmpty()) {
                        poke2.abilities.forEach { ab ->
                            val isChosen = ab.name_en == selAbilityEn
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isChosen) Color(0xFF9B59B6).copy(0.3f)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        selAbilityEn = ab.name_en
                                        selAbilityKo = ab.name_ko
                                    }
                                    .padding(4.dp)
                            ) {
                                Text(
                                    ab.name_ko,
                                    color = if (isChosen) PokeTextPri else PokeTextSec,
                                    fontSize = 10.sp, fontWeight = FontWeight.Bold
                                )
                                if (ab.is_hidden) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("(숨특)", color = PokeRed, fontSize = 8.sp)
                                }
                            }
                        }
                    } else {
                        Text("포켓몬을 먼저 선택", color = PokeTextSec, fontSize = 9.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 저장 / 취소 버튼
                Row {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(PokeBorder)
                            .clickable { onCancel() }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("취소", color = PokeTextSec, fontSize = 12.sp) }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (selectedPokemon != null && selectedMoves.isNotEmpty())
                                    PokeAccent else PokeBorder
                            )
                            .clickable {
                                val poke = selectedPokemon
                                if (poke != null && selectedMoves.isNotEmpty()) {
                                    onSave(MyPokemonSave(
                                        pokemonId = poke.id,
                                        natureUp = nature.upIndex,
                                        natureDown = nature.downIndex,
                                        evs = evs.toList(),
                                        moveIds = selectedMoves.toList(),
                                        abilityEn = selAbilityEn,
                                        abilityKo = selAbilityKo
                                    ))
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("저장", color = Color.White, fontSize = 12.sp,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 성격 팝업
        if (showNaturePopup) {
            // PanelState 임시 생성하여 NaturePopup 재사용
            val tempPanel = remember { PanelState().apply { this.nature = nature } }
            LaunchedEffect(nature) { tempPanel.nature = nature }
            NaturePopup(
                panel = tempPanel,
                onDismiss = {
                    nature = tempPanel.nature
                    showNaturePopup = false
                }
            )
        }
    }
}
