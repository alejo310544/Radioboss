import 'dart:convert';
import 'dart:io';
import 'package:audioplayers/audioplayers.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() => runApp(const RadioVozApp());

class RadioVozApp extends StatelessWidget {
  const RadioVozApp({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp(
    title: 'RadioVoz Móvil',
    debugShowCheckedModeBanner: false,
    theme: ThemeData(
      brightness: Brightness.dark,
      colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF00C389), brightness: Brightness.dark),
      scaffoldBackgroundColor: const Color(0xFF10151B),
      cardTheme: const CardThemeData(color: Color(0xFF1A222B)),
      useMaterial3: true,
    ),
    home: const RadioHome(),
  );
}

class AudioItem {
  AudioItem(this.path, {String? title}) : title = title ?? path.split(Platform.pathSeparator).last;
  final String path;
  final String title;
  Map<String, String> toJson() => {'path': path, 'title': title};
  factory AudioItem.fromJson(Map<String, dynamic> j) => AudioItem(j['path'] as String, title: j['title'] as String?);
}

class RadioHome extends StatefulWidget {
  const RadioHome({super.key});
  @override
  State<RadioHome> createState() => _RadioHomeState();
}

class _RadioHomeState extends State<RadioHome> {
  final AudioPlayer _player = AudioPlayer();
  final AudioPlayer _effectPlayer = AudioPlayer();
  final List<AudioItem> _tracks = [];
  final List<AudioItem> _effects = [];
  final List<AudioItem> _spots = [];
  final List<Map<String, String>> _schedule = [];
  int _tab = 0;
  int? _current;
  double _volume = 0.8;
  bool _duck = false;
  bool _playing = false;
  final _server = TextEditingController();
  final _port = TextEditingController(text: '8000');
  final _user = TextEditingController(text: 'source');
  final _password = TextEditingController();
  String _stationStatus = 'Sin conectar';
  String _playlistName = 'Sin playlist cargada';

  @override
  void initState() {
    super.initState();
    _loadSaved();
    _player.onPlayerComplete.listen((_) {
      if (_current != null && _current! + 1 < _tracks.length) {
        _playIndex(_current! + 1);
      } else {
        setState(() => _playing = false);
      }
    });
  }

  Future<void> _loadSaved() async {
    final p = await SharedPreferences.getInstance();
    List<AudioItem> decode(String key) {
      final raw = p.getString(key);
      if (raw == null) return [];
      try { return (jsonDecode(raw) as List).map((e) => AudioItem.fromJson(Map<String, dynamic>.from(e))).toList(); }
      catch (_) { return []; }
    }
    if (!mounted) return;
    setState(() {
      _tracks.addAll(decode('tracks'));
      _effects.addAll(decode('effects'));
      _spots.addAll(decode('spots'));
      _server.text = p.getString('server') ?? '';
      _port.text = p.getString('port') ?? '8000';
      _user.text = p.getString('user') ?? 'source';
      _password.text = p.getString('password') ?? '';
    });
  }

  Future<void> _saveLists() async {
    final p = await SharedPreferences.getInstance();
    await p.setString('tracks', jsonEncode(_tracks.map((e) => e.toJson()).toList()));
    await p.setString('effects', jsonEncode(_effects.map((e) => e.toJson()).toList()));
    await p.setString('spots', jsonEncode(_spots.map((e) => e.toJson()).toList()));
  }

  Future<void> _saveServer() async {
    final p = await SharedPreferences.getInstance();
    await p.setString('server', _server.text.trim());
    await p.setString('port', _port.text.trim());
    await p.setString('user', _user.text.trim());
    await p.setString('password', _password.text);
    if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Configuración guardada en este teléfono.')));
  }

  Future<void> _addFiles(List<AudioItem> target, {bool multiple = true}) async {
    final result = await FilePicker.platform.pickFiles(type: FileType.custom, allowedExtensions: ['mp3','wav','m4a','aac','ogg','flac'], allowMultiple: multiple, withData: false);
    if (result == null) return;
    for (final f in result.files) {
      if (f.path != null) target.add(AudioItem(f.path!, title: f.name));
    }
    await _saveLists();
    if (mounted) setState(() {});
  }

  Future<void> _importM3u() async {
    final result = await FilePicker.platform.pickFiles(type: FileType.custom, allowedExtensions: ['m3u','m3u8'], withData: true);
    if (result == null || result.files.single.bytes == null) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('No se pudo leer el archivo. Prueba con un M3U de texto UTF-8.')));
      return;
    }
    final file = result.files.single;
    final text = utf8.decode(file.bytes!, allowMalformed: true);
    final imported = <AudioItem>[];
    final base = file.path == null ? '' : File(file.path!).parent.path;
    for (final raw in const LineSplitter().convert(text)) {
      final line = raw.trim();
      if (line.isEmpty || line.startsWith('#')) continue;
      if (line.startsWith('http://') || line.startsWith('https://')) {
        imported.add(AudioItem(line, title: line.split('/').last));
      } else {
        final candidate = line.startsWith('/') ? line : (base.isEmpty ? line : '$base${Platform.pathSeparator}$line');
        if (File(candidate).existsSync()) imported.add(AudioItem(candidate));
      }
    }
    setState(() { _tracks
      ..clear()
      ..addAll(imported); _playlistName = file.name; _current = null; });
    await _saveLists();
    if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Lista cargada: ${imported.length} pistas reconocidas.')));
  }

  Future<void> _playIndex(int i) async {
    if (i < 0 || i >= _tracks.length) return;
    final item = _tracks[i];
    try {
      if (item.path.startsWith('http://') || item.path.startsWith('https://')) {
        await _player.play(UrlSource(item.path), volume: _duck ? _volume * 0.25 : _volume);
      } else {
        if (!File(item.path).existsSync()) {
          if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('No se encuentra el archivo: ${item.title}')));
          return;
        }
        await _player.play(DeviceFileSource(item.path), volume: _duck ? _volume * 0.25 : _volume);
      }
      setState(() { _current = i; _playing = true; });
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('No se pudo reproducir: $e')));
    }
  }

  Future<void> _stop() async { await _player.stop(); setState(() => _playing = false); }
  Future<void> _toggleDuck() async {
    setState(() => _duck = !_duck);
    await _player.setVolume(_duck ? _volume * 0.25 : _volume);
  }

  Future<void> _playEffect(AudioItem item) async {
    try {
      await _effectPlayer.stop();
      if (item.path.startsWith('http')) {
        await _effectPlayer.play(UrlSource(item.path));
      } else {
        if (!File(item.path).existsSync()) throw Exception('Archivo no encontrado');
        await _effectPlayer.play(DeviceFileSource(item.path));
      }
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Error en efecto: $e')));
    }
  }

  Future<void> _editTitle(AudioItem item, List<AudioItem> list) async {
    final c = TextEditingController(text: item.title);
    final ok = await showDialog<bool>(context: context, builder: (ctx) => AlertDialog(
      title: const Text('Editar nombre del botón'),
      content: TextField(controller: c, decoration: const InputDecoration(labelText: 'Nombre visible')),
      actions: [TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancelar')), FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Guardar'))],
    ));
    if (ok == true && c.text.trim().isNotEmpty) {
      final index = list.indexOf(item);
      list[index] = AudioItem(item.path, title: c.text.trim());
      await _saveLists(); setState(() {});
    }
  }

  Future<void> _addSchedule() async {
    final title = TextEditingController();
    final time = TextEditingController(text: '12:00');
    final ok = await showDialog<bool>(context: context, builder: (ctx) => AlertDialog(
      title: const Text('Nueva programación'),
      content: Column(mainAxisSize: MainAxisSize.min, children: [
        TextField(controller: title, decoration: const InputDecoration(labelText: 'Nombre (programa, cuña, etc.)')),
        TextField(controller: time, decoration: const InputDecoration(labelText: 'Hora 24 h (HH:MM)')),
      ]),
      actions: [TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancelar')), FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Añadir'))],
    ));
    if (ok == true && title.text.trim().isNotEmpty && RegExp(r'^\d{2}:\d{2}$').hasMatch(time.text.trim())) {
      setState(() => _schedule.add({'time': time.text.trim(), 'title': title.text.trim()}));
    }
  }

  @override
  void dispose() {
    _player.dispose(); _effectPlayer.dispose();
    _server.dispose(); _port.dispose(); _user.dispose(); _password.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final pages = [_console(), _playlistPage(), _soundboard(), _automation(), _streaming()];
    return Scaffold(
      appBar: AppBar(title: const Text('RadioVoz Móvil', style: TextStyle(fontWeight: FontWeight.bold)), centerTitle: false,
        actions: [Padding(padding: const EdgeInsets.only(right: 14), child: Center(child: Container(padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5), decoration: BoxDecoration(color: _playing ? Colors.green.shade800 : Colors.blueGrey.shade800, borderRadius: BorderRadius.circular(20)), child: Text(_playing ? 'AL AIRE*' : 'EN ESPERA', style: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold)))))],
      ),
      body: SafeArea(child: pages[_tab]),
      bottomNavigationBar: NavigationBar(selectedIndex: _tab, onDestinationSelected: (v) => setState(() => _tab = v), destinations: const [
        NavigationDestination(icon: Icon(Icons.radio), label: 'Consola'),
        NavigationDestination(icon: Icon(Icons.queue_music), label: 'Playlist'),
        NavigationDestination(icon: Icon(Icons.grid_view), label: 'Efectos'),
        NavigationDestination(icon: Icon(Icons.schedule), label: 'Programación'),
        NavigationDestination(icon: Icon(Icons.wifi_tethering), label: 'Streaming'),
      ]),
    );
  }

  Widget _console() => ListView(padding: const EdgeInsets.all(16), children: [
    Card(child: Padding(padding: const EdgeInsets.all(18), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      const Text('REPRODUCTOR PRINCIPAL', style: TextStyle(color: Colors.white60, fontSize: 12, letterSpacing: 1.3)),
      const SizedBox(height: 10),
      Text(_current != null && _current! < _tracks.length ? _tracks[_current!].title : 'Selecciona una pista', style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
      const SizedBox(height: 14),
      Row(children: [
        IconButton.filled(onPressed: _current != null && _current! > 0 ? () => _playIndex(_current! - 1) : null, icon: const Icon(Icons.skip_previous)),
        const SizedBox(width: 8),
        IconButton.filled(onPressed: () { if (_playing) { _player.pause(); setState(() => _playing = false); } else if (_current != null) { _player.resume(); setState(() => _playing = true); } else if (_tracks.isNotEmpty) { _playIndex(0); } }, icon: Icon(_playing ? Icons.pause : Icons.play_arrow), iconSize: 32),
        const SizedBox(width: 8),
        IconButton.filledTonal(onPressed: _stop, icon: const Icon(Icons.stop)),
        const Spacer(),
        IconButton(onPressed: _current != null && _current! + 1 < _tracks.length ? () => _playIndex(_current! + 1) : null, icon: const Icon(Icons.skip_next)),
      ]),
      const SizedBox(height: 8),
      Row(children: [const Icon(Icons.volume_down), Expanded(child: Slider(value: _volume, onChanged: (v) async { setState(() => _volume = v); await _player.setVolume(_duck ? v * .25 : v); })), const Icon(Icons.volume_up)]),
      SwitchListTile(contentPadding: EdgeInsets.zero, title: const Text('Atenuar música al hablar'), subtitle: const Text('Reduce el reproductor al 25%'), value: _duck, onChanged: (_) => _toggleDuck()),
      const Text('* Estado local del reproductor; no confirma una transmisión por internet.', style: TextStyle(color: Colors.orangeAccent, fontSize: 11)),
    ]))),
    Card(child: ListTile(leading: const Icon(Icons.queue_music), title: Text(_playlistName), subtitle: Text('${_tracks.length} pistas cargadas'), trailing: const Icon(Icons.chevron_right), onTap: () => setState(() => _tab = 1))),
    Card(child: ListTile(leading: const Icon(Icons.mic), title: const Text('Micrófono del celular'), subtitle: const Text('La captura y mezcla de micrófono aún requiere integrar el motor de transmisión.'), trailing: const Icon(Icons.info_outline))),
    Card(child: ListTile(leading: const Icon(Icons.wifi_tethering), title: const Text('Listen2MyRadio'), subtitle: Text(_stationStatus), onTap: () => setState(() => _tab = 4))),
  ]);

  Widget _playlistPage() => ListView(padding: const EdgeInsets.all(16), children: [
    const Text('PLAYLISTS', style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
    const SizedBox(height: 6), const Text('Importa M3U/M3U8 o añade archivos de audio locales.'),
    const SizedBox(height: 14),
    Wrap(spacing: 8, runSpacing: 8, children: [
      FilledButton.icon(onPressed: _importM3u, icon: const Icon(Icons.playlist_add), label: const Text('Importar M3U')),
      OutlinedButton.icon(onPressed: () => _addFiles(_tracks), icon: const Icon(Icons.audio_file), label: const Text('Añadir audios')),
      OutlinedButton.icon(onPressed: () async { final path = await FilePicker.platform.getDirectoryPath(); if (path != null) { final d = Directory(path); final fs = d.listSync().whereType<File>().where((f) => ['mp3','wav','m4a','aac','ogg','flac'].contains(f.path.split('.').last.toLowerCase())); setState(() { _tracks.addAll(fs.map((f) => AudioItem(f.path))); _playlistName = 'Carpeta: ${d.path.split(Platform.pathSeparator).last}'; }); await _saveLists(); } }, icon: const Icon(Icons.folder_open), label: const Text('Añadir carpeta')),
    ]),
    const SizedBox(height: 12),
    if (_tracks.isEmpty) const Card(child: Padding(padding: EdgeInsets.all(24), child: Center(child: Text('Todavía no hay pistas.')))),
    ..._tracks.asMap().entries.map((entry) => Card(child: ListTile(
      leading: CircleAvatar(child: Text('${entry.key + 1}')),
      title: Text(entry.value.title, maxLines: 1, overflow: TextOverflow.ellipsis),
      subtitle: Text(entry.value.path, maxLines: 1, overflow: TextOverflow.ellipsis),
      onTap: () => _playIndex(entry.key),
      trailing: PopupMenuButton<String>(onSelected: (v) async {
        if (v == 'play') await _playIndex(entry.key);
        if (v == 'remove') { setState(() => _tracks.removeAt(entry.key)); await _saveLists(); }
      }, itemBuilder: (_) => const [PopupMenuItem(value: 'play', child: Text('Reproducir')), PopupMenuItem(value: 'remove', child: Text('Quitar de la lista'))]),
    ))),
    const SizedBox(height: 12),
    OutlinedButton.icon(onPressed: () async {
      final content = ['#EXTM3U', ..._tracks.map((e) => e.path)].join('\n');
      await showDialog<void>(context: context, builder: (ctx) => AlertDialog(title: const Text('Contenido M3U'), content: SingleChildScrollView(child: SelectableText(content)), actions: [TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cerrar'))]));
    }, icon: const Icon(Icons.code), label: const Text('Ver lista M3U para exportar')),
  ]);

  Widget _soundboard() => ListView(padding: const EdgeInsets.all(16), children: [
    const Text('BOTONERA DE EFECTOS', style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
    const Text('Cada botón reproduce el archivo asignado. Puedes añadir sonidos y cambiar el nombre visible.'),
    const SizedBox(height: 14),
    FilledButton.icon(onPressed: () => _addFiles(_effects), icon: const Icon(Icons.add), label: const Text('Añadir archivos a la botonera')),
    const SizedBox(height: 12),
    if (_effects.isEmpty) const Card(child: Padding(padding: EdgeInsets.all(24), child: Center(child: Text('Añade aplausos, cortinas, identificaciones o efectos.')))),
    GridView.builder(shrinkWrap: true, physics: const NeverScrollableScrollPhysics(), gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 2, childAspectRatio: 1.25, crossAxisSpacing: 10, mainAxisSpacing: 10), itemCount: _effects.length, itemBuilder: (_, i) {
      final e = _effects[i];
      return Card(child: InkWell(borderRadius: BorderRadius.circular(12), onTap: () => _playEffect(e), onLongPress: () => _editTitle(e, _effects), child: Padding(padding: const EdgeInsets.all(12), child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
        const Icon(Icons.graphic_eq, size: 30, color: Color(0xFF00C389)), const SizedBox(height: 8),
        Text(e.title, textAlign: TextAlign.center, maxLines: 2, overflow: TextOverflow.ellipsis),
        const SizedBox(height: 6), const Text('Tocar para reproducir · Mantener para editar', textAlign: TextAlign.center, style: TextStyle(fontSize: 9, color: Colors.white54)),
      ]))));
    }),
  ]);

  Widget _automation() => ListView(padding: const EdgeInsets.all(16), children: [
    const Text('PROGRAMACIÓN', style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
    const SizedBox(height: 6),
    const Text('Organiza programas, cuñas y eventos por hora. En esta versión los horarios se guardan en pantalla mientras la app está abierta; la ejecución automática en segundo plano se integrará en otra fase.'),
    const SizedBox(height: 14),
    FilledButton.icon(onPressed: _addSchedule, icon: const Icon(Icons.add_alarm), label: const Text('Añadir evento')),
    const SizedBox(height: 12),
    if (_schedule.isEmpty) const Card(child: Padding(padding: EdgeInsets.all(24), child: Center(child: Text('No hay eventos programados.')))),
    ..._schedule.map((s) => Card(child: ListTile(leading: const Icon(Icons.schedule), title: Text(s['title']!), subtitle: Text('Hora: ${s['time']}'), trailing: IconButton(icon: const Icon(Icons.delete_outline), onPressed: () => setState(() => _schedule.remove(s)))))),
    Card(child: ListTile(leading: const Icon(Icons.campaign), title: const Text('Cuñas y comerciales'), subtitle: Text('${_spots.length} audios cargados'), trailing: IconButton(icon: const Icon(Icons.add), onPressed: () => _addFiles(_spots)))),
    ..._spots.map((s) => ListTile(leading: const Icon(Icons.audiotrack), title: Text(s.title), trailing: IconButton(icon: const Icon(Icons.delete_outline), onPressed: () async { setState(() => _spots.remove(s)); await _saveLists(); }))),
  ]);

  Widget _streaming() => ListView(padding: const EdgeInsets.all(16), children: [
    const Text('TRANSMISIÓN POR INTERNET', style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
    const SizedBox(height: 8),
    const Text('Guarda aquí los datos del servidor. Esta versión inicial todavía no envía audio a Listen2MyRadio; no mostrará “conectado” hasta integrar y probar un encoder compatible con el tipo de servidor de tu cuenta.'),
    const SizedBox(height: 16),
    TextField(controller: _server, decoration: const InputDecoration(labelText: 'Servidor o IP', hintText: 'Ej. servidor.radio.com', prefixIcon: Icon(Icons.dns))),
    const SizedBox(height: 10),
    TextField(controller: _port, keyboardType: TextInputType.number, decoration: const InputDecoration(labelText: 'Puerto', prefixIcon: Icon(Icons.numbers))),
    const SizedBox(height: 10),
    TextField(controller: _user, decoration: const InputDecoration(labelText: 'Usuario / mount si corresponde', prefixIcon: Icon(Icons.person_outline))),
    const SizedBox(height: 10),
    TextField(controller: _password, obscureText: true, decoration: const InputDecoration(labelText: 'Contraseña', prefixIcon: Icon(Icons.lock_outline))),
    const SizedBox(height: 16),
    FilledButton.icon(onPressed: _saveServer, icon: const Icon(Icons.save), label: const Text('Guardar configuración')),
    const SizedBox(height: 10),
    OutlinedButton.icon(onPressed: () => showDialog<void>(context: context, builder: (ctx) => AlertDialog(title: const Text('Motor de transmisión pendiente'), content: const Text('Para conectar de verdad necesitamos confirmar si tu cuenta usa Icecast, Shoutcast u otro endpoint y qué formato de audio admite. No compartas la contraseña aquí; bastan el tipo de servidor, puerto y formato requerido.'), actions: [TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Entendido'))])), icon: const Icon(Icons.info_outline), label: const Text('Requisitos de conexión')),
    const SizedBox(height: 16),
    Card(child: ListTile(leading: const Icon(Icons.shield_outlined), title: const Text('Privacidad'), subtitle: const Text('La configuración se guarda localmente en este teléfono. Evita subir contraseñas al repositorio de GitHub.'))),
  ]);
}
