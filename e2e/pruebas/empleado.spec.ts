import { expect, test } from '@playwright/test'

import { ApiAdmin, correoNuevo, hoyEnZona, registrarCliente, sumarDias, unico } from '../utilidades/api.ts'
import { ADMIN_EMAIL, ADMIN_PASSWORD, PASSWORD } from '../utilidades/entorno.ts'
import { iniciarSesion, reservarComoCliente } from '../utilidades/web.ts'

// CU-016 y CU-017 (el admin), RN-005 (contraseña temporal), CU-021 y CU-013 a CU-015 (el empleado).

test('el admin da de alta un servicio y un empleado, que atiende y cancela una cita', async ({ browser }) => {
  const nombreServicio = unico('Tinte')
  const nombreEmpleado = unico('Emp').replace(' ', '')
  const emailEmpleado = correoNuevo('empleado')
  const fecha = sumarDias(hoyEnZona(), 3)
  let servicioId = 0
  let empleadoId = 0
  let passwordTemporal = ''

  const admin = await (await browser.newContext()).newPage()
  await iniciarSesion(admin, ADMIN_EMAIL, ADMIN_PASSWORD)

  await test.step('el admin crea el servicio (CU-016)', async () => {
    await admin.getByRole('navigation', { name: 'Principal' }).getByRole('link', { name: 'Servicios' }).click()
    await admin.getByRole('link', { name: 'Nuevo servicio' }).click()
    await admin.getByLabel('Nombre').fill(nombreServicio)
    await admin.getByLabel('Duración').selectOption({ label: '30 min' })
    await admin.getByLabel('Precio (€)').fill('18,50')
    await admin.getByRole('button', { name: 'Crear servicio' }).click()
    await expect(admin.getByText('Servicio creado.')).toBeVisible()
    servicioId = Number(/\/admin\/servicios\/(\d+)/.exec(admin.url())?.[1])
  })

  await test.step('el admin da de alta al empleado con ese servicio (CU-017)', async () => {
    await admin.getByRole('navigation', { name: 'Principal' }).getByRole('link', { name: 'Empleados' }).click()
    await admin.getByRole('link', { name: 'Dar de alta' }).click()
    await admin.getByLabel('Nombre').fill(nombreEmpleado)
    await admin.getByLabel('Apellidos').fill('Prueba')
    await admin.getByLabel('Correo electrónico').fill(emailEmpleado)
    await admin.getByLabel(`${nombreServicio} (30 min)`).check()
    await admin.getByRole('button', { name: 'Dar de alta' }).click()
    await expect(admin.getByRole('heading', { name: 'Empleado dado de alta' })).toBeVisible()
    passwordTemporal = (await admin.locator('code').textContent()) ?? ''
    expect(passwordTemporal).not.toBe('')
    const ficha = await admin.getByRole('link', { name: 'Ver su ficha' }).getAttribute('href')
    empleadoId = Number(/\/admin\/empleados\/(\d+)/.exec(ficha ?? '')?.[1])
  })

  // El horario se prueba en horarios.spec.ts; aquí basta con que trabaje todos los días.
  const api = await ApiAdmin.abrir()
  await api.horarioCompleto(empleadoId)
  await api.cerrar()

  const empleado = await (await browser.newContext()).newPage()

  await test.step('el empleado entra con la temporal y la cambia (RN-005)', async () => {
    await empleado.goto('/login')
    await empleado.getByLabel('Correo electrónico').fill(emailEmpleado)
    await empleado.getByLabel('Contraseña', { exact: true }).fill(passwordTemporal)
    await empleado.getByRole('button', { name: 'Iniciar sesión' }).click()
    await expect(empleado).toHaveURL(/\/cambiar-password$/)
    await expect(empleado.getByText('Estás usando una contraseña temporal.')).toBeVisible()
    await empleado.getByLabel('Contraseña temporal').fill(passwordTemporal)
    await empleado.getByLabel('Contraseña nueva', { exact: true }).fill(PASSWORD)
    await empleado.getByLabel('Repite la contraseña nueva').fill(PASSWORD)
    await empleado.getByRole('button', { name: 'Guardar contraseña' }).click()
    await expect(empleado.getByText('Contraseña cambiada correctamente.')).toBeVisible()
    await expect(empleado.getByRole('heading', { name: 'Mi agenda', level: 1 })).toBeVisible()
  })

  await test.step('ve el servicio en "Mis servicios" (CU-021)', async () => {
    await empleado.getByRole('navigation', { name: 'Principal' }).getByRole('link', { name: 'Mis servicios' }).click()
    await expect(empleado.getByRole('heading', { name: nombreServicio })).toBeVisible()
    await expect(empleado.getByText('18,50 €')).toBeVisible()
  })

  const paginaCliente = await (await browser.newContext()).newPage()
  const cliente = await registrarCliente(undefined, 'Clara')
  let detalleCliente = ''

  await test.step('un cliente le reserva una hora', async () => {
    await iniciarSesion(paginaCliente, cliente.email)
    detalleCliente = await reservarComoCliente(paginaCliente, servicioId, empleadoId, fecha, '12:00')
  })

  await test.step('el empleado la ve en su agenda y la cancela con motivo (CU-013 a CU-015)', async () => {
    await empleado.goto(`/empleado/agenda?desde=${fecha}`)
    const cita = empleado.getByRole('link', { name: new RegExp(`12:00–12:30 ${nombreServicio}`) })
    await expect(cita).toContainText(`${cliente.nombre} ${cliente.apellidos}`)
    await cita.click()
    await expect(empleado.getByRole('heading', { name: nombreServicio, level: 1 })).toBeVisible()
    await empleado.getByRole('button', { name: 'Cancelar cita' }).click()
    await empleado.getByLabel('Motivo').fill('El profesional está enfermo')
    await empleado.getByRole('button', { name: 'Sí, cancelar' }).click()
    await expect(empleado.getByText('Cita cancelada.')).toBeVisible()
  })

  await test.step('el cliente ve la cita cancelada y el motivo', async () => {
    await paginaCliente.goto(detalleCliente)
    await expect(paginaCliente.getByText('El profesional está enfermo')).toBeVisible()
  })
})
