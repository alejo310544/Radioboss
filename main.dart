import 'package:flutter/material.dart';

void main() => runApp(RadioVozApp());

class RadioVozApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'RadioVoz Móvil',
      theme: ThemeData.dark(),
      home: PantallaPrincipal(),
    );
  }
}

class PantallaPrincipal extends StatefulWidget {
  @override
  _PantallaPrincipalState createState() => _PantallaPrincipalState();
}

class _PantallaPrincipalState extends State<PantallaPrincipal> with SingleTickerProviderStateMixin {
  late TabController _tabController;
  bool alAire = false;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Estéreo 101.3 FM - Panel de Emisión'),
        bottom: TabBar(
          controller: _tabController,
          tabs: [
            Tab(icon: Icon(Icons.radio), text: 'Al Aire / Cuñero'),
            Tab(icon: Icon(Icons.featured_play_list), text: 'Playlists'),
            Tab(icon: Icon(Icons.alarm), text: 'Eventos'),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          _construirCuñero(),
          _construirPlaylists(),
          _construirEventos(),
        ],
      ),
      bottomNavigationBar: Container(
        padding: EdgeInsets.all(16),
        color: Colors.black45,
        child: ElevatedButton(
          style: ElevatedButton.styleFrom(
            backgroundColor: alAire ? Colors.red : Colors.green,
            minimumSize: Size(double.infinity, 50)
          ),
          onPressed: () {
            setState(() { alAire = !alAire; });
          },
          child: Text(alAire ? '🔴 DETENER TRANSMISIÓN' : '📡 TRANSMITIR EN VIVO (SHOUTCAST)'),
        ),
      ),
    );
  }

  Widget _construirCuñero() {
    return Column(
      children: [
        Padding(
          padding: EdgeInsets.all(16),
          child: Card(
            child: ListTile(
              leading: Icon(Icons.music_note, color: Colors.blue),
              title: Text("Canción Actual en Reproducción"),
              subtitle: Text("Canal A - Lista Principal"),
            ),
          ),
        ),
        Expanded(
          child: GridView.builder(
            padding: EdgeInsets.all(16),
            gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 3, crossAxisSpacing: 10, mainAxisSpacing: 10),
            itemCount: 12,
            itemBuilder: (context, index) {
              return ElevatedButton(
                style: ElevatedButton.styleFrom(backgroundColor: Colors.amber),
                onPressed: () {},
                onLongPress: () {},
                child: Text('Cuña ${index + 1}\n(Mantén presionado)'),
              );
            },
          ),
        )
      ],
    );
  }

  Widget _construirPlaylists() {
    return Center(child: Text("Administrador de Carpetas y Música de tu Celular"));
  }

  Widget _construirEventos() {
    return Center(child: Text("Programador Horario de Cuñas y Automatización"));
  }
}
