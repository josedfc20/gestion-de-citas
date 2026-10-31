/** Lee un id de la URL: un número entero positivo, o undefined si falta o no es válido. */
export function numeroDeParametro(valor: string | null): number | undefined {
  if (!valor || !/^\d+$/.test(valor)) return undefined
  const numero = Number(valor)
  return numero > 0 ? numero : undefined
}

/** Lee una fecha AAAA-MM-DD de la URL, o undefined si no tiene ese formato. */
export function fechaDeParametro(valor: string | null): string | undefined {
  return valor && /^\d{4}-\d{2}-\d{2}$/.test(valor) ? valor : undefined
}
