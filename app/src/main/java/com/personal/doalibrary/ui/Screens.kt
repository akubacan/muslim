@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.personal.doalibrary.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.personal.doalibrary.data.PrayerEntity
import com.personal.doalibrary.data.UiSettings
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private fun alignOf(a: String) = when (a) {
    "center" -> TextAlign.Center; "left" -> TextAlign.Left; "justify" -> TextAlign.Justify; else -> TextAlign.Right
}

@Composable
fun ArabicText(t: String, s: UiSettings, modifier: Modifier = Modifier) = Text(
    t, modifier.fillMaxWidth(), color = arabColor(),
    style = TextStyle(
        fontFamily = FontFamily.Serif, fontSize = s.arSize.sp, lineHeight = (s.arSize * s.arLine).sp,
        textAlign = alignOf(s.arAlign), textDirection = TextDirection.Rtl
    )
)

@Composable
fun TranslationText(t: String, s: UiSettings, modifier: Modifier = Modifier) = Text(
    t, modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onBackground,
    style = TextStyle(
        fontSize = s.trSize.sp, lineHeight = (s.trSize * s.trLine).sp,
        textAlign = alignOf(s.trAlign), textDirection = TextDirection.Ltr
    )
)

@Composable
fun AppRoot(vm: MainViewModel = hiltViewModel()) {
    val nav = rememberNavController()
    val prayers by vm.prayers.collectAsState()
    val trash by vm.trash.collectAsState()
    val st by vm.settings.collectAsState()
    AppTheme(st.theme) {
        val route = nav.currentBackStackEntryAsState().value?.destination?.route
        val tabs = listOf(
            Triple("home", "Home", Icons.Default.Home),
            Triple("lib", "Kumpulan Doa", Icons.Default.Menu),
            Triple("settings", "Pengaturan", Icons.Default.Settings)
        )
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (route in tabs.map { it.first }) NavigationBar {
                    tabs.forEach { (r, l, ic) ->
                        NavigationBarItem(
                            selected = route == r,
                            onClick = { nav.navigate(r) { popUpTo("home"); launchSingleTop = true } },
                            icon = { Icon(ic, l) }, label = { Text(l, maxLines = 1) }
                        )
                    }
                }
            }
        ) { pad ->
            NavHost(nav, "home", Modifier.padding(pad)) {
                composable("home") {
                    HomeScreen(prayers, { nav.navigate("lib") }, { nav.navigate("edit") },
                        { nav.navigate("settings") }, { nav.navigate("read/$it") })
                }
                composable("lib") {
                    LibraryScreen(prayers, vm, { nav.navigate("read/$it") },
                        { id -> nav.navigate(if (id == null) "edit" else "edit?id=$id") }, { nav.navigate("trash") })
                }
                composable("edit?id={id}", arguments = listOf(navArgument("id") {
                    type = NavType.StringType; nullable = true; defaultValue = null
                })) { e ->
                    EditorScreen(e.arguments?.getString("id"), prayers, st, vm) { nav.popBackStack() }
                }
                composable("read/{id}") { e ->
                    ReaderScreen(e.arguments?.getString("id") ?: "", prayers, st) { nav.popBackStack() }
                }
                composable("settings") { SettingsScreen(st, vm) { nav.navigate("trash") } }
                composable("trash") { TrashScreen(trash, vm) { nav.popBackStack() } }
            }
        }
    }
}

@Composable
fun HomeScreen(
    prayers: List<PrayerEntity>, onLib: () -> Unit, onAdd: () -> Unit,
    onSettings: () -> Unit, onOpen: (String) -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Personal Doa Library", style = MaterialTheme.typography.headlineMedium)
        Text("Assalamu’alaikum. Tempat tenang untuk doa-doamu.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("${prayers.size}", style = MaterialTheme.typography.displayMedium)
                Text("doa tersimpan · ${prayers.count { it.isFavorite }} favorit",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onLib) { Text("Kumpulan Doa") }
            OutlinedButton(onAdd) { Text("Tambah Doa") }
        }
        TextButton(onSettings) { Text("Pengaturan") }
        val recent = prayers.sortedByDescending { it.updatedAt }.take(4)
        Text(if (recent.isEmpty()) "Belum ada doa. Mulai dengan Tambah Doa." else "Terbaru",
            style = MaterialTheme.typography.titleMedium)
        recent.forEach { p ->
            OutlinedCard(onClick = { onOpen(p.id) }, Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(p.title, Modifier.weight(1f))
                    if (p.isFavorite) Icon(Icons.Default.Favorite, "Favorit", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun LibraryScreen(
    prayers: List<PrayerEntity>, vm: MainViewModel,
    onOpen: (String) -> Unit, onEdit: (String?) -> Unit, onTrash: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var favOnly by rememberSaveable { mutableStateOf(false) }
    var sorting by remember { mutableStateOf(false) }
    val order = remember { mutableStateListOf<String>() }
    var toDelete by remember { mutableStateOf<PrayerEntity?>(null) }
    val byId = prayers.associateBy { it.id }
    val stepPx = with(LocalDensity.current) { 72.dp.toPx() }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (sorting) "Urutkan Doa" else "Kumpulan Doa", Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall)
            if (!sorting) FilledTonalButton({ onEdit(null) }) { Icon(Icons.Default.Add, null); Text("Tambah") }
        }
        if (!sorting) {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("Cari judul doa") }, leadingIcon = { Icon(Icons.Default.Search, null) })
            Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                FilterChip(favOnly, { favOnly = !favOnly }, { Text("Favorit") })
                OutlinedButton(
                    onClick = { order.clear(); order.addAll(prayers.map { it.id }); sorting = true },
                    enabled = prayers.size > 1 && query.isBlank() && !favOnly
                ) { Text("Urutkan Doa") }
                TextButton(onTrash) { Text("Sampah") }
            }
        } else {
            Text("Tekan dan geser ikon ≡ ke atas atau bawah.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ vm.reorder(order.toList()); sorting = false }) { Text("Simpan") }
                OutlinedButton({ sorting = false }) { Text("Batal") }
            }
        }
        val shown = if (sorting) order.mapNotNull { byId[it] }
        else prayers.filter { (!favOnly || it.isFavorite) && it.title.contains(query, ignoreCase = true) }

        if (shown.isEmpty()) Text(
            if (prayers.isEmpty()) "Belum ada doa. Tekan Tambah." else "Tidak ada doa yang cocok.",
            Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyColumn(Modifier.fillMaxSize()) {
            itemsIndexed(shown, key = { _, p -> p.id }) { i, p ->
                val number = if (sorting) i + 1 else prayers.indexOfFirst { it.id == p.id } + 1
                Card(Modifier.fillMaxWidth().height(72.dp).padding(vertical = 4.dp).animateItemPlacement()) {
                    Row(Modifier.fillMaxSize().padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("%02d".format(number), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton({ if (!sorting) onOpen(p.id) }, Modifier.weight(1f)) {
                            Text(p.title, Modifier.fillMaxWidth(), maxLines = 2, textAlign = TextAlign.Start,
                                color = MaterialTheme.colorScheme.onSurface)
                        }
                        if (sorting) {
                            Icon(Icons.Default.Menu, "Geser untuk mengurutkan",
                                Modifier.padding(14.dp).pointerInput(p.id) {
                                    var acc = 0f
                                    detectDragGestures(onDragStart = { acc = 0f }) { change, amt ->
                                        change.consume(); acc += amt.y
                                        val idx = order.indexOf(p.id)
                                        if (acc > stepPx && idx < order.lastIndex) {
                                            order.add(idx + 1, order.removeAt(idx)); acc -= stepPx
                                        } else if (acc < -stepPx && idx > 0) {
                                            order.add(idx - 1, order.removeAt(idx)); acc += stepPx
                                        }
                                    }
                                })
                        } else {
                            IconButton({ vm.toggleFav(p) }) {
                                Icon(if (p.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorit",
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton({ onEdit(p.id) }) { Icon(Icons.Default.Edit, "Edit") }
                            IconButton({ toDelete = p }) { Icon(Icons.Default.Delete, "Hapus") }
                        }
                    }
                }
            }
        }
    }
    toDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Hapus doa?") },
            text = { Text("“${p.title}” akan dipindahkan ke tempat sampah dan bisa dipulihkan.") },
            confirmButton = { TextButton({ vm.moveToTrash(p.id); toDelete = null }) { Text("Pindahkan") } },
            dismissButton = { TextButton({ toDelete = null }) { Text("Batal") } }
        )
    }
}

@Composable
fun EditorScreen(id: String?, prayers: List<PrayerEntity>, s: UiSettings, vm: MainViewModel, onDone: () -> Unit) {
    val orig = remember(id) { prayers.find { it.id == id } }
    var title by rememberSaveable { mutableStateOf(orig?.title ?: "") }
    var ar by rememberSaveable { mutableStateOf(orig?.arabicText ?: "") }
    var tr by rememberSaveable { mutableStateOf(orig?.translation ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmLeave by remember { mutableStateOf(false) }
    val dirty = title != (orig?.title ?: "") || ar != (orig?.arabicText ?: "") || tr != (orig?.translation ?: "")
    BackHandler(dirty) { confirmLeave = true }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (id == null) "Tambah Doa" else "Edit Doa", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Judul doa") }, singleLine = true)
        OutlinedTextField(ar, { ar = it }, Modifier.fillMaxWidth(), label = { Text("Teks Arab (dengan harakat)") },
            minLines = 4, textStyle = TextStyle(fontFamily = FontFamily.Serif, fontSize = 24.sp, lineHeight = 44.sp,
                textDirection = TextDirection.Rtl, textAlign = TextAlign.Right))
        OutlinedTextField(tr, { tr = it }, Modifier.fillMaxWidth(), label = { Text("Terjemahan") },
            minLines = 4, textStyle = TextStyle(textDirection = TextDirection.Ltr))
        Text("Preview", style = MaterialTheme.typography.titleMedium)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(title.ifBlank { "(tanpa judul)" }, style = MaterialTheme.typography.titleLarge)
                ArabicText(ar, s); TranslationText(tr, s)
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({
                if (title.isBlank() || ar.isBlank() || tr.isBlank()) error = "Judul, teks Arab, dan terjemahan wajib diisi."
                else vm.save(id, title.trim(), ar.trim(), tr.trim(), onDone)
            }) { Text("Simpan") }
            OutlinedButton({ if (dirty) confirmLeave = true else onDone() }) { Text("Batal") }
        }
    }
    if (confirmLeave) AlertDialog(
        onDismissRequest = { confirmLeave = false },
        title = { Text("Buang perubahan?") },
        text = { Text("Perubahan yang belum disimpan akan hilang.") },
        confirmButton = { TextButton(onDone) { Text("Buang") } },
        dismissButton = { TextButton({ confirmLeave = false }) { Text("Lanjut mengedit") } }
    )
}

@Composable
fun ReaderScreen(id: String, prayers: List<PrayerEntity>, s: UiSettings, onBack: () -> Unit) {
    if (prayers.isEmpty()) { LaunchedEffect(Unit) { onBack() }; return }
    val pager = rememberPagerState(initialPage = prayers.indexOfFirst { it.id == id }.coerceAtLeast(0)) { prayers.size }
    val scope = rememberCoroutineScope()
    val page = pager.currentPage.coerceIn(0, prayers.lastIndex)
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") }
            Spacer(Modifier.weight(1f))
            Text("%02d/%02d".format(page + 1, prayers.size), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
        }
        HorizontalPager(pager, Modifier.weight(1f)) { i ->
            val p = prayers.getOrNull(i)
            if (p != null) Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Text(p.title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
                ArabicText(p.arabicText, s)
                HorizontalDivider(Modifier.padding(vertical = 16.dp))
                TranslationText(p.translation, s)
                Spacer(Modifier.height(24.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton({ scope.launch { pager.animateScrollToPage(page - 1) } }, enabled = page > 0) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null); Text("Sebelumnya")
            }
            OutlinedButton({ scope.launch { pager.animateScrollToPage(page + 1) } }, enabled = page < prayers.lastIndex) {
                Text("Berikutnya"); Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
            }
        }
    }
}

@Composable
fun SettingsScreen(s: UiSettings, vm: MainViewModel, onTrash: () -> Unit) {
    val aligns = listOf("right" to "Kanan", "center" to "Tengah", "left" to "Kiri", "justify" to "Rata")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Pengaturan", style = MaterialTheme.typography.headlineSmall)
        Text("Tema", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("system" to "Sistem", "light" to "Terang", "dark" to "Gelap").forEach { (k, l) ->
                FilterChip(s.theme == k, { vm.setSettings(s.copy(theme = k)) }, { Text(l) })
            }
        }
        Text("Tulisan Arab", style = MaterialTheme.typography.titleMedium)
        Text("Ukuran: ${s.arSize}")
        Slider(s.arSize.toFloat(), { vm.setSettings(s.copy(arSize = it.roundToInt())) }, valueRange = 18f..56f)
        Text("Jarak baris: %.1f".format(s.arLine))
        Slider(s.arLine, { vm.setSettings(s.copy(arLine = (it * 10).roundToInt() / 10f)) }, valueRange = 1.2f..3f)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            aligns.forEach { (k, l) -> FilterChip(s.arAlign == k, { vm.setSettings(s.copy(arAlign = k)) }, { Text(l) }) }
        }
        ArabicText("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", s)
        Text("Terjemahan", style = MaterialTheme.typography.titleMedium)
        Text("Ukuran: ${s.trSize}")
        Slider(s.trSize.toFloat(), { vm.setSettings(s.copy(trSize = it.roundToInt())) }, valueRange = 12f..32f)
        Text("Jarak baris: %.1f".format(s.trLine))
        Slider(s.trLine, { vm.setSettings(s.copy(trLine = (it * 10).roundToInt() / 10f)) }, valueRange = 1.1f..2.4f)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            aligns.forEach { (k, l) -> FilterChip(s.trAlign == k, { vm.setSettings(s.copy(trAlign = k)) }, { Text(l) }) }
        }
        TranslationText("Dengan nama Allah Yang Maha Pengasih, Maha Penyayang.", s)
        HorizontalDivider()
        OutlinedButton(onTrash) { Text("Tempat Sampah") }
        Text("Font Manager, tema custom, dan backup/restore belum tersedia di versi ini.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun TrashScreen(trash: List<PrayerEntity>, vm: MainViewModel, onBack: () -> Unit) {
    var purge by remember { mutableStateOf<PrayerEntity?>(null) }
    var purgeAll by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") }
            Text("Tempat Sampah", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
            if (trash.isNotEmpty()) TextButton({ purgeAll = true }) { Text("Kosongkan") }
        }
        if (trash.isEmpty()) Text("Sampah kosong.", Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn {
            itemsIndexed(trash, key = { _, p -> p.id }) { _, p ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(p.title, style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton({ vm.restore(p.id) }) { Text("Pulihkan") }
                            TextButton({ purge = p }) { Text("Hapus permanen", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }
    purge?.let { p -> AlertDialog(
        onDismissRequest = { purge = null }, title = { Text("Hapus permanen?") },
        text = { Text("“${p.title}” tidak bisa dipulihkan lagi.") },
        confirmButton = { TextButton({ vm.purge(p.id); purge = null }) { Text("Hapus permanen") } },
        dismissButton = { TextButton({ purge = null }) { Text("Batal") } }) }
    if (purgeAll) AlertDialog(
        onDismissRequest = { purgeAll = false }, title = { Text("Kosongkan sampah?") },
        text = { Text("Semua doa di sampah dihapus permanen.") },
        confirmButton = { TextButton({ vm.purgeAll(); purgeAll = false }) { Text("Hapus semua") } },
        dismissButton = { TextButton({ purgeAll = false }) { Text("Batal") } })
}
