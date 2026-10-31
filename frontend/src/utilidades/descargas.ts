/** Guarda un Blob como archivo con un enlace temporal (lo usa "Descargar mis datos"). */
export function guardarArchivo(contenido: Blob, nombre: string): void {
  const direccion = URL.createObjectURL(contenido)
  const enlace = document.createElement('a')
  enlace.href = direccion
  enlace.download = nombre
  document.body.append(enlace)
  enlace.click()
  enlace.remove()
  // Se libera después: algunos navegadores aún están leyendo el archivo.
  setTimeout(() => URL.revokeObjectURL(direccion), 1000)
}
