"""Regenere les fichiers de routes de docs/final-frontend-doc depuis les classes compilees.

Usage, depuis la racine d'un dossier du depot ou le code est compile (voir README.md) :
    python tools/frontend-doc/generate.py [dossier de la doc]
"""
import os
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))


def main():
    out_dir = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, 'docs', 'final-frontend-doc')
    classes = os.path.join(ROOT, 'target', 'classes')
    if not os.path.isdir(classes):
        sys.exit('target/classes absent : compiler d\'abord (./mvnw -o -q compile).')
    work = tempfile.mkdtemp(prefix='frontend-doc-')
    classpath_file = os.path.join(work, 'classpath.txt')
    mvnw = os.path.join(ROOT, 'mvnw.cmd' if os.name == 'nt' else 'mvnw')
    subprocess.run([mvnw, '-o', '-q', 'dependency:build-classpath', f'-Dmdep.outputFile={classpath_file}'],
                   cwd=ROOT, check=True)
    classpath = open(classpath_file, encoding='utf-8').read().strip()
    # La liste depasse la longueur d'une ligne de commande Windows : elle passe par un fichier.
    args_file = os.path.join(work, 'java.args')
    with open(args_file, 'w', encoding='utf-8') as handle:
        handle.write('-cp\n"' + (classes + os.pathsep + classpath).replace('\\', '/') + '"\n')
    java_home = os.environ.get('JAVA_HOME')
    java = os.path.join(java_home, 'bin', 'java') if java_home else 'java'
    catalog = os.path.join(work, 'routes.json')
    subprocess.run([java, '@' + args_file, os.path.join(HERE, 'RouteCatalog.java'), catalog], cwd=ROOT, check=True)
    subprocess.run([sys.executable, os.path.join(HERE, 'render.py'), catalog, out_dir], check=True)


if __name__ == '__main__':
    main()
