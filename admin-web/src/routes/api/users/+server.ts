import { json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import { createClient } from '@supabase/supabase-js';
import { PUBLIC_SUPABASE_URL, PUBLIC_SUPABASE_ANON_KEY } from '$env/static/public';
import { SUPABASE_SERVICE_ROLE_KEY } from '$env/static/private';

const serviceClient = createClient(PUBLIC_SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY, {
	auth: { autoRefreshToken: false, persistSession: false }
});

type CallerProfile = {
	id: string;
	role: 'super_admin' | 'admin' | 'kasir';
	store_id: string | null;
	is_active: boolean;
};

async function getCaller(request: Request): Promise<CallerProfile | null> {
	const token = request.headers.get('Authorization')?.replace('Bearer ', '');
	if (!token) return null;
	const { data } = await serviceClient.auth.getUser(token);
	if (!data.user) return null;
	const { data: profile } = await serviceClient
		.from('profiles')
		.select('id, role, store_id, is_active')
		.eq('id', data.user.id)
		.single();
	return profile ?? null;
}

export const POST: RequestHandler = async ({ request }) => {
	const caller = await getCaller(request);
	if (!caller) return json({ error: 'Tidak terautentikasi' }, { status: 401 });
	if (!caller.is_active) return json({ error: 'Akun nonaktif' }, { status: 403 });
	if (caller.role === 'kasir') return json({ error: 'Akses ditolak' }, { status: 403 });

	let body: {
		email?: string;
		password?: string;
		full_name?: string;
		role?: string;
		store_id?: string | null;
	};
	try {
		body = await request.json();
	} catch {
		return json({ error: 'Body tidak valid' }, { status: 400 });
	}

	const email = body.email?.trim().toLowerCase();
	const password = body.password;
	const targetRole = body.role ?? 'kasir';
	const storeId = body.store_id || null;

	if (!email || !password || password.length < 6) {
		return json({ error: 'Email dan password (min. 6 karakter) wajib diisi' }, { status: 400 });
	}
	if (!['admin', 'kasir'].includes(targetRole)) {
		return json({ error: 'Role harus admin atau kasir' }, { status: 400 });
	}

	// Admin toko hanya boleh membuat kasir di tokonya sendiri
	if (caller.role === 'admin') {
		if (targetRole !== 'kasir') {
			return json({ error: 'Admin hanya boleh membuat user kasir' }, { status: 403 });
		}
		if (!storeId || storeId !== caller.store_id) {
			return json({ error: 'Kasir wajib ditempatkan di toko Anda' }, { status: 403 });
		}
	} else if (!storeId) {
		return json({ error: 'Toko wajib dipilih' }, { status: 400 });
	}

	// Pastikan email belum terdaftar
	const { data: existing } = await serviceClient.auth.admin.listUsers({
		page: 1,
		perPage: 1
	});
	const duplicate = existing?.users?.some((u) => u.email?.toLowerCase() === email);
	if (duplicate) {
		return json({ error: 'Email sudah terdaftar' }, { status: 409 });
	}

	const { data: created, error } = await serviceClient.auth.admin.createUser({
		email,
		password,
		email_confirm: true,
		app_metadata: { approved: true },
		user_metadata: {
			full_name: body.full_name?.trim() ?? '',
			role: targetRole,
			store_id: storeId
		}
	});

	if (error) return json({ error: error.message }, { status: 400 });
	return json({ id: created.user?.id });
};

export const DELETE: RequestHandler = async ({ url, request }) => {
	const caller = await getCaller(request);
	if (!caller) return json({ error: 'Tidak terautentikasi' }, { status: 401 });
	if (!caller.is_active) return json({ error: 'Akun nonaktif' }, { status: 403 });

	const userId = url.searchParams.get('userId');
	if (!userId) return json({ error: 'userId wajib diisi' }, { status: 400 });

	const { data: target } = await serviceClient
		.from('profiles')
		.select('id, role, store_id')
		.eq('id', userId)
		.single();

	if (!target) return json({ error: 'User tidak ditemukan' }, { status: 404 });

	if (target.role === 'super_admin' && caller.role !== 'super_admin') {
		return json({ error: 'Hanya super admin boleh menghapus super admin' }, { status: 403 });
	}
	if (caller.role === 'admin') {
		if (target.store_id !== caller.store_id || !['admin', 'kasir'].includes(target.role)) {
			return json({ error: 'Akses ditolak untuk user ini' }, { status: 403 });
		}
	}

	const { error } = await serviceClient.auth.admin.deleteUser(userId);
	if (error) return json({ error: error.message }, { status: 400 });

	// Profil ikut terhapus via ON DELETE CASCADE dari auth.users
	return json({ ok: true });
};
