import React, { useEffect, useRef } from 'react';
import * as THREE from 'three';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js';
import { AnatomyScene } from '../types';

interface Props {
  scene: AnatomyScene;
  activeAssemblyId?: string;
  onSelect: (assemblyId: string) => void;
}

const materialColor: Record<string, number> = {
  GLASS: 0x42d8ff,
  CHROME: 0x9c6cff,
  STEEL: 0x4ed4a8,
  BRASS: 0xffb84d,
  MATTE_DARK: 0x34435f
};

export const WebGLArchitectureCanvas: React.FC<Props> = ({ scene, activeAssemblyId, onSelect }) => {
  const hostRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const host = hostRef.current;
    if (!host) return;

    const world = new THREE.Scene();
    world.background = new THREE.Color(0x07111f);
    const camera = new THREE.PerspectiveCamera(44, host.clientWidth / host.clientHeight, 0.1, 2000);
    camera.position.set(0, 220, 430);
    const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.setSize(host.clientWidth, host.clientHeight);
    host.replaceChildren(renderer.domElement);

    const controls = new OrbitControls(camera, renderer.domElement);
    controls.enableDamping = true;
    controls.target.set(260, 30, 120);
    controls.minDistance = 180;
    controls.maxDistance = 900;

    world.add(new THREE.AmbientLight(0x6d8cff, 1.1));
    const key = new THREE.DirectionalLight(0x8eeaff, 2.2);
    key.position.set(180, 300, 260);
    world.add(key);
    const floor = new THREE.GridHelper(760, 28, 0x19385e, 0x10233d);
    floor.position.set(260, -5, 125);
    world.add(floor);

    const meshes = new Map<string, THREE.Mesh>();
    const positions = new Map<string, THREE.Vector3>();
    for (const assembly of scene.assemblies) {
      const width = Math.max(28, assembly.width * 0.68);
      const height = Math.max(20, assembly.height * 0.68);
      const depth = Math.max(28, assembly.depth * 0.68);
      const isActive = assembly.id === activeAssemblyId;
      const material = new THREE.MeshStandardMaterial({
        color: materialColor[assembly.material] || 0x7086ab,
        emissive: isActive ? 0x284cff : 0x000000,
        emissiveIntensity: isActive ? 1.25 : 0.2,
        metalness: assembly.material === 'GLASS' ? 0.15 : 0.55,
        roughness: assembly.material === 'GLASS' ? 0.18 : 0.34,
        transparent: assembly.material === 'GLASS',
        opacity: assembly.material === 'GLASS' ? 0.78 : 1
      });
      const mesh = new THREE.Mesh(new THREE.BoxGeometry(width, height, depth), material);
      mesh.position.set(assembly.x, assembly.z + height / 2, assembly.y);
      mesh.userData.assemblyId = assembly.id;
      meshes.set(assembly.id, mesh);
      positions.set(assembly.id, mesh.position);
      world.add(mesh);

      const edges = new THREE.LineSegments(new THREE.EdgesGeometry(mesh.geometry), new THREE.LineBasicMaterial({ color: 0xc8ddff }));
      edges.position.copy(mesh.position);
      world.add(edges);
    }

    for (const connection of scene.connections) {
      const start = positions.get(connection.sourceAssemblyId);
      const end = positions.get(connection.targetAssemblyId);
      if (!start || !end) continue;
      const geometry = new THREE.BufferGeometry().setFromPoints([start, end]);
      const line = new THREE.Line(geometry, new THREE.LineBasicMaterial({ color: 0x39f5b8, transparent: true, opacity: 0.75 }));
      world.add(line);
    }

    const raycaster = new THREE.Raycaster();
    const pointer = new THREE.Vector2();
    const click = (event: MouseEvent) => {
      const bounds = renderer.domElement.getBoundingClientRect();
      pointer.x = ((event.clientX - bounds.left) / bounds.width) * 2 - 1;
      pointer.y = -((event.clientY - bounds.top) / bounds.height) * 2 + 1;
      raycaster.setFromCamera(pointer, camera);
      const hit = raycaster.intersectObjects([...meshes.values()], false)[0];
      if (hit?.object.userData.assemblyId) onSelect(hit.object.userData.assemblyId as string);
    };
    renderer.domElement.addEventListener('click', click);

    const resize = () => {
      camera.aspect = host.clientWidth / host.clientHeight;
      camera.updateProjectionMatrix();
      renderer.setSize(host.clientWidth, host.clientHeight);
    };
    window.addEventListener('resize', resize);
    let frame = 0;
    const animate = () => {
      frame = requestAnimationFrame(animate);
      controls.update();
      renderer.render(world, camera);
    };
    animate();

    return () => {
      cancelAnimationFrame(frame);
      renderer.domElement.removeEventListener('click', click);
      window.removeEventListener('resize', resize);
      world.traverse((object) => {
        if (object instanceof THREE.Mesh || object instanceof THREE.LineSegments || object instanceof THREE.Line) {
          object.geometry.dispose();
          const material = object.material;
          if (Array.isArray(material)) material.forEach((item) => item.dispose());
          else material.dispose();
        }
      });
      controls.dispose();
      renderer.dispose();
    };
  }, [scene, activeAssemblyId, onSelect]);

  return <div ref={hostRef} className="h-full min-h-[420px] w-full rounded-xl overflow-hidden" aria-label="Interactive WebGL architecture canvas" />;
};
