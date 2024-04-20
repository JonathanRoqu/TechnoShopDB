package com.example.technoshopdb;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class lista_productos extends AppCompatActivity {
    Bundle parametros = new Bundle();
    DB dbproductos;
    ListView lts;
    Cursor cProductos;
    FloatingActionButton btn;
    productos misProductos;
    final ArrayList<productos> alProductos=new ArrayList<productos>();
    final ArrayList<productos> alProductosCopy=new ArrayList<productos>();
    JSONArray datosJSON;
    JSONObject jsonObject;
    obtenerDatosServidor datosServidor;
    detectarInternet di;
    int posicion=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lista_productos);
        dbproductos = new DB(lista_productos.this, "", null, 1);

        btn = findViewById(R.id.fabAgregarProductos);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                parametros.putString("accion", "nuevo");
                abrirActividad(parametros);
            }
        });
        try{
            di = new detectarInternet(getApplicationContext());
            if(di.hayConexionInternet()){
                obtenerDatosProductosServidor();
            }else{
                obtenerProductos();
            }
        }catch (Exception e){
            mostrarMsg("Error al detectar si hay conexion "+ e.getMessage());
        }
        //obtenerProductos();
        buscarProductos();
    }
    private void obtenerDatosProductosServidor(){
        try{
            datosServidor = new obtenerDatosServidor();
            String data = datosServidor.execute().get();
            jsonObject = new JSONObject(data);
            datosJSON = jsonObject.getJSONArray("rows");
            mostrarDatosProductos();
        }catch (Exception e){
            mostrarMsg("Error al obtener datos desde el servidor: "+ e.getMessage());
        }
    }
    private void mostrarDatosProductos(){
        try{
            if( datosJSON.length()>0){
                lts = findViewById(R.id.ltsProductos);

                alProductos.clear();
                alProductosCopy.clear();

                JSONObject misDatosJSONObject;
                for (int i=0; i<datosJSON.length(); i++){
                    misDatosJSONObject = datosJSON.getJSONObject(i).getJSONObject("value");
                    misProductos = new productos(
                            misDatosJSONObject.getString("_id"),
                            misDatosJSONObject.getString("_rev"),
                            misDatosJSONObject.getString("idProducto"),
                            misDatosJSONObject.getString("codigo"),
                            misDatosJSONObject.getString("descripcion"),
                            misDatosJSONObject.getString("marca"),
                            misDatosJSONObject.getString("presentacion"),
                            misDatosJSONObject.getString("precio"),
                            misDatosJSONObject.getString("urlCompletaFoto")
                    );
                    alProductos.add(misProductos);
                }
                adaptadorImagenes adImagenes = new adaptadorImagenes(getApplicationContext(), alProductos);
                lts.setAdapter(adImagenes);
                alProductosCopy.addAll(alProductos);

                registerForContextMenu(lts);
            }else{
                mostrarMsg("No hay Datos que mostrar");
            }
        } catch (JSONException e) {
            mostrarMsg("Error al mostrar los datos: "+e.getMessage());

        }
    }
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mimenu, menu);
try{
    AdapterView.AdapterContextMenuInfo info =(AdapterView.AdapterContextMenuInfo) menuInfo;
    posicion = info.position;
    menu.setHeaderTitle(datosJSON.getJSONObject(posicion).getJSONObject("value").getString("nombre"));
}catch (Exception e){
    mostrarMsg("Error al mostrar el menu: "+ e.getMessage());
}

    }
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        try {
            switch (item.getItemId()){
                case R.id.mnxAgregar:
                    parametros.putString("accion", "nuevo");
                    abrirActividad(parametros);
                    break;
                case R.id.mnxModificar:
                    parametros.putString("accion","modificar");
                    parametros.putString("productos", datosJSON.getJSONObject(posicion).toString());
                    abrirActividad(parametros);
                    break;
                case R.id.mnxEliminar:
                    eliminarProductos();
                    break;
            }
            return true;
        }catch (Exception e){
            mostrarMsg("Error en menu: "+ e.getMessage());
            return super.onContextItemSelected(item);
        }
    }
    private void eliminarProductos(){
        try {
            AlertDialog.Builder confirmacion = new AlertDialog.Builder(lista_productos.this);
            confirmacion.setTitle("Esta seguro de Eliminar a: ");
            confirmacion.setMessage(datosJSON.getJSONObject(posicion).getJSONObject("value").getString("nombre"));
            confirmacion.setPositiveButton("SI", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    try {
                        String respuesta = dbproductos.administrar_productos("eliminar", new String[]{"", "", datosJSON.getJSONObject(posicion).getJSONObject("value").getString("idAmigo")});
                        if (respuesta.equals("ok")) {
                            mostrarMsg("Producto eliminado con exito.");
                            obtenerProductos();
                        } else {
                            mostrarMsg("Error al eliminar producto: " + respuesta);
                        }
                    } catch (Exception e) {
                        mostrarMsg("Error al eliminar Datos: " + e.getMessage());
                    }
                }
            });
            confirmacion.setNegativeButton("NO", new DialogInterface.OnClickListener(){
                    @Override
                  public void onClick(DialogInterface dialogInterface, int i) {

                    }
            });
            confirmacion.create().show();
        }catch (Exception e){
            mostrarMsg("Error al eliminar: "+ e.getMessage());
        }
    }
    private void abrirActividad(Bundle parametros){
        Intent abriVentana = new Intent(getApplicationContext(), MainActivity.class);
        abriVentana.putExtras(parametros);
        startActivity(abriVentana);
    }
    private void obtenerProductos(){
        try{
          cProductos = dbproductos.obtener_productos();
          if ( cProductos.moveToFirst() ){
              datosJSON = new JSONArray();
              do{
                  jsonObject = new JSONObject();
                  JSONObject jsonObjectValue = new JSONObject();
                  jsonObject.put( "_id",cProductos.getString( 0));
                  jsonObject.put( "_rev",cProductos.getString( 1));
                  jsonObject.put( "idProducto",cProductos.getString( 2));
                  jsonObject.put( "codigo",cProductos.getString( 3));
                  jsonObject.put( "descripcion",cProductos.getString( 4));
                  jsonObject.put( "marca",cProductos.getString( 5));
                  jsonObject.put( "presentacion",cProductos.getString( 6));
                  jsonObject.put( "precio",cProductos.getString( 7));
                  jsonObject.put( "urlFotoCompleta",cProductos.getString( 8));

                  jsonObjectValue.put( "value", jsonObject);
                  datosJSON.put(jsonObjectValue);
              }while(cProductos.moveToNext());
              mostrarDatosProductos();
          }else{
              mostrarMsg("No hay productos que mostrar");
          }

        }catch (Exception e){
            mostrarMsg("Error al obtener productos: "+ e.getMessage());
        }
    }
    private void mostrarMsg(String msg){
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }
    private void buscarProductos(){
        TextView tempVal;
        tempVal = findViewById(R.id.txtBuscarProductos);
        tempVal.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    alProductos.clear();
                    String valor = tempVal.getText().toString().trim().toLowerCase();
                    if( valor.length()<=0 ){
                        alProductos.addAll(alProductosCopy);
                    }else{
                        for( productos producto : alProductosCopy ){
                            String codigo = producto.getCodigo();
                            String descripcion = producto.getDescripcion();
                            String marca = producto.getMarca();
                            String presentacion = producto.getPresentacion();
                            if( codigo.toLowerCase().trim().contains(valor) ||
                                    descripcion.toLowerCase().trim().contains(valor) ||
                                    marca.trim().contains(valor) ||
                                    presentacion.trim().toLowerCase().contains(valor) ){
                                alProductos.add(producto);
                            }
                        }
                        adaptadorImagenes adImagenes = new adaptadorImagenes(getApplicationContext(), alProductos);
                        lts.setAdapter(adImagenes);
                    }
                }catch (Exception e){
                    mostrarMsg("Error al buscar: "+e.getMessage() );
                }
            }
            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }
}