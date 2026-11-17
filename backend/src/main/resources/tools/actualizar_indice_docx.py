#!/usr/bin/env python3
"""Recalcula índices de un DOCX con el motor de maquetación de LibreOffice."""

import os
import socket
import subprocess
import sys
import tempfile
import time
from pathlib import Path

import uno
from com.sun.star.beans import PropertyValue


def property_value(name, value):
    value_property = PropertyValue()
    value_property.Name = name
    value_property.Value = value
    return value_property


def libreoffice_context(port):
    local_context = uno.getComponentContext()
    resolver = local_context.ServiceManager.createInstanceWithContext(
        "com.sun.star.bridge.UnoUrlResolver", local_context
    )
    connection = f"uno:socket,host=127.0.0.1,port={port};urp;StarOffice.ComponentContext"
    last_error = None
    for _ in range(120):
        try:
            return resolver.resolve(connection)
        except Exception as error:  # LibreOffice may still be starting.
            last_error = error
            time.sleep(0.5)
    raise RuntimeError(f"LibreOffice no respondió a tiempo: {last_error}")


def main():
    if len(sys.argv) != 3:
        raise RuntimeError("Uso: actualizar_indice_docx.py entrada.docx salida.docx")

    source = Path(sys.argv[1]).resolve()
    target = Path(sys.argv[2]).resolve()
    target.parent.mkdir(parents=True, exist_ok=True)

    with socket.socket() as available_port:
        available_port.bind(("127.0.0.1", 0))
        port = available_port.getsockname()[1]

    profile = Path(tempfile.mkdtemp(prefix="aring-libreoffice-"))
    profile_url = uno.systemPathToFileUrl(str(profile))
    office = subprocess.Popen(
        [
            "soffice",
            "--headless",
            "--norestore",
            "--nodefault",
            "--nofirststartwizard",
            f"-env:UserInstallation={profile_url}",
            f"--accept=socket,host=127.0.0.1,port={port};urp;StarOffice.ServiceManager",
        ],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
        env={**os.environ, "HOME": str(profile), "SAL_USE_VCLPLUGIN": "svp"},
    )
    document = None
    try:
        context = libreoffice_context(port)
        desktop = context.ServiceManager.createInstanceWithContext(
            "com.sun.star.frame.Desktop", context
        )
        document = desktop.loadComponentFromURL(
            uno.systemPathToFileUrl(str(source)),
            "_blank",
            0,
            (
                property_value("Hidden", True),
                property_value("ReadOnly", False),
                property_value("UpdateDocMode", 3),
            ),
        )
        if document is None:
            raise RuntimeError("LibreOffice no pudo abrir el informe generado.")

        indexes = document.getDocumentIndexes()
        if indexes.getCount() == 0:
            raise RuntimeError("El informe no contiene un índice que pueda actualizarse.")
        for index in range(indexes.getCount()):
            indexes.getByIndex(index).update()
        document.getTextFields().refresh()
        for index in range(indexes.getCount()):
            indexes.getByIndex(index).update()

        document.storeAsURL(
            uno.systemPathToFileUrl(str(target)),
            (
                property_value("FilterName", "Office Open XML Text"),
                property_value("Overwrite", True),
            ),
        )
        document.close(True)
        document = None
        if not target.is_file() or target.stat().st_size == 0:
            raise RuntimeError("LibreOffice no guardó el informe actualizado.")
    finally:
        if document is not None:
            document.close(True)
        office.terminate()
        try:
            office.wait(timeout=10)
        except subprocess.TimeoutExpired:
            office.kill()
            office.wait(timeout=5)
        for child in profile.iterdir():
            if child.is_dir():
                import shutil
                shutil.rmtree(child, ignore_errors=True)
            else:
                child.unlink(missing_ok=True)
        profile.rmdir()


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
